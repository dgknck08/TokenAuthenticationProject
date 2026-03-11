package com.example.ecommerce.auth.service;
import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.repository.AuditLogRepository;
import com.example.ecommerce.common.trace.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
@Service
@Transactional
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    @Autowired
    @Lazy
    private AuditService selfProxy;
    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.selfProxy = this;
    }
    public void logAuthEvent(Long userId, String username, AuditLog.AuditAction action,
                             String description, HttpServletRequest request) {
        selfProxy.logAuthEventAsync(userId, username, action, description, snapshotRequest(request));
    }
    @Async("auditExecutor")
    public CompletableFuture<Void> logAuthEventAsync(Long userId, String username, AuditLog.AuditAction action,
                                                     String description, RequestAuditInfo requestInfo) {
        try {
            AuditLog.AuditLogBuilder auditBuilder = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .description(description);
            if (requestInfo != null) {
                auditBuilder
                        .ipAddress(requestInfo.ipAddress())
                        .userAgent(requestInfo.userAgent());
                Map<String, Object> details = new HashMap<>();
                details.put("requestUri", requestInfo.requestUri());
                details.put("method", requestInfo.method());
                details.put("sessionId", requestInfo.sessionId());
                enrichAuditDetails(details, "AUTH");
                auditBuilder.details(objectMapper.writeValueAsString(details));
            }
            auditLogRepository.save(auditBuilder.build());
            logger.info("Audit event logged: {} for user: {}", action, sanitizeForLog(username));
            return CompletableFuture.completedFuture(null);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing audit details", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Error logging audit event", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    public void logSystemEvent(Long userId, String username, AuditLog.AuditAction action, String description, Map<String, Object> details) {
        selfProxy.logSystemEventAsync(userId, username, action, description, details);
    }
    @Async("auditExecutor")
    public CompletableFuture<Void> logSystemEventAsync(Long userId, String username, AuditLog.AuditAction action,
                                                       String description, Map<String, Object> details) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .description(description);
            Map<String, Object> normalizedDetails = new HashMap<>();
            if (details != null) {
                normalizedDetails.putAll(details);
            }
            enrichAuditDetails(normalizedDetails, "SYSTEM");
            if (!normalizedDetails.isEmpty()) {
                builder.details(objectMapper.writeValueAsString(normalizedDetails));
            }
            auditLogRepository.save(builder.build());
            logger.info("System audit event logged: {} by user: {}", action, sanitizeForLog(username));
            return CompletableFuture.completedFuture(null);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing system audit details", e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            logger.error("Error logging system audit event", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    public Page<AuditLog> getUserAuditLogs(String username, Pageable pageable) {
        return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username, pageable);
    }
    private RequestAuditInfo snapshotRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return new RequestAuditInfo(
                getClientIpAddress(request),
                request.getHeader("User-Agent"),
                request.getRequestURI(),
                request.getMethod(),
                request.getSession(false) != null ? request.getSession(false).getId() : null
        );
    }
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    private void enrichAuditDetails(Map<String, Object> details, String category) {
        String correlationId = CorrelationIdContext.get();
        details.putIfAbsent("eventCategory", category);
        if (correlationId != null && !correlationId.isBlank()) {
            details.putIfAbsent("correlationId", correlationId);
        }
    }
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\n\\r\\t]", "_");
    }
    public record RequestAuditInfo(String ipAddress, String userAgent, String requestUri, String method, String sessionId) {
    }
}