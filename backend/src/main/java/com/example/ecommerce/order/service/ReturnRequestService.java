package com.example.ecommerce.order.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.order.dto.ReturnRequestResponse;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.exception.ReturnRequestNotFoundException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.ReturnRequest;
import com.example.ecommerce.order.model.ReturnRequestStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.ReturnRequestRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ReturnRequestService {
    private static final Logger logger = LoggerFactory.getLogger(ReturnRequestService.class);

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public ReturnRequestService(ReturnRequestRepository returnRequestRepository,
                                OrderRepository orderRepository,
                                AuditService auditService,
                                MeterRegistry meterRegistry) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    public ReturnRequestResponse createMyReturnRequest(String username, Long orderId, String reason) {
        long startNanos = System.nanoTime();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (!order.getUsername().equals(username)) {
            recordMetric("create", "failed", startNanos);
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            recordMetric("create", "failed", startNanos);
            throw new IllegalArgumentException("Return request can only be created for delivered orders.");
        }
        if (returnRequestRepository.findByOrder_Id(orderId).isPresent()) {
            recordMetric("create", "failed", startNanos);
            throw new IllegalArgumentException("Return request already exists for this order.");
        }

        String sanitizedReason = trimOrNull(reason);
        if (sanitizedReason == null) {
            recordMetric("create", "failed", startNanos);
            throw new IllegalArgumentException("Return reason is required.");
        }

        ReturnRequest entity = new ReturnRequest();
        entity.setOrder(order);
        entity.setUserId(order.getUserId());
        entity.setUsername(order.getUsername());
        entity.setStatus(ReturnRequestStatus.REQUESTED);
        entity.setReason(sanitizedReason);
        ReturnRequest saved = returnRequestRepository.save(entity);

        recordMetric("create", "success", startNanos);
        logAudit(saved, username, AuditLog.AuditAction.ORDER_RETURN_REQUESTED);
        logger.info("event=return_request_created returnRequestId={} orderId={} username={}", saved.getId(), orderId, username);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> getMyReturnRequests(String username, Pageable pageable) {
        return returnRequestRepository.findByUsernameOrderByCreatedAtDesc(username, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> getAllReturnRequestsForAdmin(Pageable pageable) {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public ReturnRequestResponse approveReturnRequest(Long returnRequestId, String adminUsername) {
        long startNanos = System.nanoTime();
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ReturnRequestNotFoundException("Return request not found: " + returnRequestId));
        if (request.getStatus() != ReturnRequestStatus.REQUESTED) {
            recordMetric("approve", "failed", startNanos);
            throw new IllegalArgumentException("Only requested return requests can be approved.");
        }
        request.setStatus(ReturnRequestStatus.APPROVED);
        request.setReviewedBy(trimOrNull(adminUsername));
        request.setReviewedAt(Instant.now());
        ReturnRequest saved = returnRequestRepository.save(request);
        recordMetric("approve", "success", startNanos);
        logAudit(saved, adminUsername, AuditLog.AuditAction.ORDER_RETURN_APPROVED);
        logger.info("event=return_request_approved returnRequestId={} orderId={} admin={}",
                saved.getId(), saved.getOrder().getId(), adminUsername);
        return toResponse(saved);
    }

    public ReturnRequestResponse rejectReturnRequest(Long returnRequestId, String adminUsername, String adminNote) {
        long startNanos = System.nanoTime();
        ReturnRequest request = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ReturnRequestNotFoundException("Return request not found: " + returnRequestId));
        if (request.getStatus() != ReturnRequestStatus.REQUESTED) {
            recordMetric("reject", "failed", startNanos);
            throw new IllegalArgumentException("Only requested return requests can be rejected.");
        }
        String sanitizedNote = trimOrNull(adminNote);
        if (sanitizedNote == null) {
            recordMetric("reject", "failed", startNanos);
            throw new IllegalArgumentException("Rejection note is required.");
        }
        request.setStatus(ReturnRequestStatus.REJECTED);
        request.setAdminNote(sanitizedNote);
        request.setReviewedBy(trimOrNull(adminUsername));
        request.setReviewedAt(Instant.now());
        ReturnRequest saved = returnRequestRepository.save(request);
        recordMetric("reject", "success", startNanos);
        logAudit(saved, adminUsername, AuditLog.AuditAction.ORDER_RETURN_REJECTED);
        logger.info("event=return_request_rejected returnRequestId={} orderId={} admin={}",
                saved.getId(), saved.getOrder().getId(), adminUsername);
        return toResponse(saved);
    }

    private ReturnRequestResponse toResponse(ReturnRequest request) {
        return ReturnRequestResponse.builder()
                .id(request.getId())
                .orderId(request.getOrder() != null ? request.getOrder().getId() : null)
                .userId(request.getUserId())
                .username(request.getUsername())
                .status(request.getStatus())
                .reason(request.getReason())
                .adminNote(request.getAdminNote())
                .reviewedBy(request.getReviewedBy())
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private void logAudit(ReturnRequest request, String actorUsername, AuditLog.AuditAction action) {
        Map<String, Object> details = new HashMap<>();
        details.put("returnRequestId", request.getId());
        details.put("orderId", request.getOrder() != null ? request.getOrder().getId() : null);
        details.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        details.put("reason", request.getReason());
        details.put("adminNote", request.getAdminNote());
        details.put("reviewedBy", request.getReviewedBy());
        auditService.logSystemEvent(request.getUserId(), actorUsername, action, "Return request event", details);
    }

    private void recordMetric(String action, String outcome, long startNanos) {
        meterRegistry.counter("ecommerce.order.return.events", "action", action, "outcome", outcome).increment();
        Timer.builder("ecommerce.order.return.action.duration")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
