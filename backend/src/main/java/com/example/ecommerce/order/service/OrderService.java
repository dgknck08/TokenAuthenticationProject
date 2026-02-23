package com.example.ecommerce.order.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.dto.OrderItemResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.dto.PayOrderRequest;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService,
                        AuditService auditService,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    public OrderResponse createOrder(String username, CreateOrderRequest request) {
        long startNanos = System.nanoTime();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Order order = new Order();
        order.setUserId(user.getId());
        order.setUsername(user.getUsername());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

            inventoryService.ensureAvailableStock(product.getId(), itemRequest.getQuantity());
            int newAvailable = Math.max(0, inventoryService.getAvailableStock(product.getId()) - itemRequest.getQuantity());
            inventoryService.setStock(product.getId(), newAvailable);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setProductNameSnapshot(product.getName());
            order.getItems().add(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        recordOrderMetric("create", "success", startNanos);
        logOrderAudit(user.getId(), username, AuditLog.AuditAction.ORDER_CREATED, saved);
        logger.info("event=order_created orderId={} username={} totalAmount={}", saved.getId(), username, saved.getTotalAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String username) {
        return orderRepository.findByUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String username, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (!order.getUsername().equals(username)) {
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        return toResponse(order);
    }

    public OrderResponse payMyOrder(String username, Long orderId, PayOrderRequest request) {
        long startNanos = System.nanoTime();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (!order.getUsername().equals(username)) {
            recordOrderMetric("pay", "failed", startNanos);
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            recordOrderMetric("pay", "failed", startNanos);
            meterRegistry.counter("ecommerce.order.payment.failed", "reason", "invalid_state").increment();
            auditService.logSystemEvent(order.getUserId(), username, AuditLog.AuditAction.ORDER_PAYMENT_FAILED,
                    "Order payment failed", Map.of("orderId", orderId, "status", String.valueOf(order.getStatus())));
            throw new IllegalArgumentException("Only created orders can be paid.");
        }
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("pay", "success", startNanos);
        logOrderAudit(order.getUserId(), username, AuditLog.AuditAction.ORDER_PAID, saved);
        logger.info("event=order_paid orderId={} username={} paymentMethod={}", saved.getId(), username, saved.getPaymentMethod());
        return toResponse(saved);
    }

    public OrderResponse cancelOrderForAdmin(Long orderId, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            recordOrderMetric("cancel", "failed", startNanos);
            throw new IllegalArgumentException("Only created orders can be cancelled.");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("cancel", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_CANCELLED, saved);
        logger.info("event=order_cancelled orderId={} admin={}", saved.getId(), adminUsername);
        return toResponse(saved);
    }

    public OrderResponse refundOrderForAdmin(Long orderId, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PAID) {
            recordOrderMetric("refund", "failed", startNanos);
            throw new IllegalArgumentException("Only paid orders can be refunded.");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("refund", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_REFUNDED, saved);
        logger.info("event=order_refunded orderId={} admin={}", saved.getId(), adminUsername);
        return toResponse(saved);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            int current = inventoryService.getAvailableStock(productId);
            inventoryService.setStock(productId, current + item.getQuantity());
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> {
                    Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
                    String productName = item.getProductNameSnapshot() != null
                            ? item.getProductNameSnapshot()
                            : (item.getProduct() != null ? item.getProduct().getName() : null);
                    BigDecimal unitPrice = item.getUnitPrice() != null
                            ? item.getUnitPrice()
                            : (item.getProduct() != null && item.getProduct().getPrice() != null
                                    ? item.getProduct().getPrice()
                                    : BigDecimal.ZERO);
                    return OrderItemResponse.builder()
                            .productId(productId)
                            .productName(productName)
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .lineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .username(order.getUsername())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paidAt(order.getPaidAt())
                .cancelledAt(order.getCancelledAt())
                .refundedAt(order.getRefundedAt())
                .build();
    }

    private void recordOrderMetric(String action, String outcome, long startNanos) {
        meterRegistry.counter("ecommerce.order.events", "action", action, "outcome", outcome).increment();
        Timer.builder("ecommerce.order.action.duration")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }

    private void logOrderAudit(Long userId, String actorUsername, AuditLog.AuditAction action, Order order) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", order.getId());
        details.put("status", order.getStatus().name());
        details.put("totalAmount", order.getTotalAmount());
        details.put("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        details.put("itemCount", order.getItems().size());
        auditService.logSystemEvent(userId, actorUsername, action, "Order event", details);
    }
}
