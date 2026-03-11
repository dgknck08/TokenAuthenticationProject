package com.example.ecommerce.order.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.order.dto.CheckoutQuoteRequest;
import com.example.ecommerce.order.dto.CheckoutQuoteResponse;
import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.dto.OrderItemResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.dto.ShipOrderRequest;
import com.example.ecommerce.order.dto.ShippingAddressRequest;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final CheckoutPricingService checkoutPricingService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService,
                        CheckoutPricingService checkoutPricingService,
                        AuditService auditService,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.checkoutPricingService = checkoutPricingService;
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
        OrderPricingResult pricing = checkoutPricingService.buildPricing(
                request.getItems(),
                user.getId(),
                request.getCouponCode(),
                request.getShippingMethod()
        );
        order.setSubtotalAmount(pricing.subtotalAmount());
        order.setDiscountAmount(pricing.discountAmount());
        order.setShippingFee(pricing.shippingFee());
        order.setTaxAmount(pricing.taxAmount());
        order.setTotalAmount(pricing.totalAmount());
        order.setCouponCode(pricing.appliedCouponCode());
        order.setShippingMethod(pricing.shippingMethod());
        applyShippingAddress(order, request.getShippingAddress());

        for (OrderPricingItem pricingItem : pricing.items()) {
            Product product = pricingItem.product();
            int quantity = pricingItem.quantity();
            inventoryService.decreaseStockWithOptimisticLock(product.getId(), quantity);
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setProductNameSnapshot(product.getName());
            order.getItems().add(item);
        }
        Order saved = orderRepository.save(order);
        recordOrderMetric("create", "success", startNanos);
        logOrderAudit(user.getId(), username, AuditLog.AuditAction.ORDER_CREATED, saved);
        logger.info("event=order_created orderId={} username={} totalAmount={}", saved.getId(), username, saved.getTotalAmount());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse quoteCheckout(String username, CheckoutQuoteRequest request) {
        long startNanos = System.nanoTime();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        OrderPricingResult pricing = checkoutPricingService.buildPricing(
                request.getItems(),
                user.getId(),
                request.getCouponCode(),
                request.getShippingMethod()
        );
        recordOrderMetric("quote", "success", startNanos);
        auditService.logSystemEvent(
                user.getId(),
                username,
                AuditLog.AuditAction.CHECKOUT_QUOTED,
                "Checkout quote calculated",
                Map.of(
                        "subtotalAmount", pricing.subtotalAmount(),
                        "discountAmount", pricing.discountAmount(),
                        "shippingFee", pricing.shippingFee(),
                        "taxAmount", pricing.taxAmount(),
                        "totalAmount", pricing.totalAmount(),
                        "couponCode", pricing.appliedCouponCode()
                )
        );
        return CheckoutQuoteResponse.builder()
                .subtotalAmount(pricing.subtotalAmount())
                .discountAmount(pricing.discountAmount())
                .shippingFee(pricing.shippingFee())
                .taxAmount(pricing.taxAmount())
                .totalAmount(pricing.totalAmount())
                .appliedCouponCode(pricing.appliedCouponCode())
                .shippingMethod(pricing.shippingMethod())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String username, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUsernameOrderByCreatedAtDesc(username, pageable);
        preloadOrderItemsAndProducts(orders.getContent());
        return orders.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersForAdmin(Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        preloadOrderItemsAndProducts(orders.getContent());
        return orders.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(String username, Long orderId) {
        Order order = loadOrderWithItems(orderId);
        if (!order.getUsername().equals(username)) {
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        return toResponse(order);
    }

    public OrderResponse cancelMyOrder(String username, Long orderId) {
        return cancelMyOrder(username, orderId, null);
    }

    public OrderResponse cancelMyOrder(String username, Long orderId, String cancelReason) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
        if (!order.getUsername().equals(username)) {
            recordOrderMetric("cancel_customer", "failed", startNanos);
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            recordOrderMetric("cancel_customer", "failed", startNanos);
            throw new IllegalArgumentException("Only created orders can be cancelled by customer.");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(trimOrNull(cancelReason));
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("cancel_customer", "success", startNanos);
        logOrderAudit(order.getUserId(), username, AuditLog.AuditAction.ORDER_CANCELLED, saved);
        logger.info("event=order_cancelled_by_customer orderId={} username={}", saved.getId(), username);
        return toResponse(saved);
    }

    public OrderResponse cancelOrderForAdmin(Long orderId, String adminUsername) {
        return cancelOrderForAdmin(orderId, adminUsername, null);
    }

    public OrderResponse cancelOrderForAdmin(Long orderId, String adminUsername, String cancelReason) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            recordOrderMetric("cancel", "failed", startNanos);
            throw new IllegalArgumentException("Only created orders can be cancelled.");
        }
        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(trimOrNull(cancelReason));
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("cancel", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_CANCELLED, saved);
        logger.info("event=order_cancelled orderId={} admin={}", saved.getId(), adminUsername);
        return toResponse(saved);
    }

    public OrderResponse refundOrderForAdmin(Long orderId, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
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

    public OrderResponse packOrderForAdmin(Long orderId, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
        if (order.getStatus() != OrderStatus.PAID) {
            recordOrderMetric("pack", "failed", startNanos);
            throw new IllegalArgumentException("Only paid orders can be packed.");
        }
        order.setStatus(OrderStatus.PACKED);
        order.setPackedAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("pack", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_PACKED, saved);
        logger.info("event=order_packed orderId={} admin={}", saved.getId(), adminUsername);
        return toResponse(saved);
    }

    public OrderResponse shipOrderForAdmin(Long orderId, ShipOrderRequest request, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
        if (order.getStatus() != OrderStatus.PACKED) {
            recordOrderMetric("ship", "failed", startNanos);
            throw new IllegalArgumentException("Only packed orders can be shipped.");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trimOrNull(request.getTrackingNumber()));
        order.setShippedAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("ship", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_SHIPPED, saved);
        logger.info("event=order_shipped orderId={} admin={} trackingNumber={}", saved.getId(), adminUsername, saved.getTrackingNumber());
        return toResponse(saved);
    }

    public OrderResponse deliverOrderForAdmin(Long orderId, String adminUsername) {
        long startNanos = System.nanoTime();
        Order order = loadOrderWithItems(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            recordOrderMetric("deliver", "failed", startNanos);
            throw new IllegalArgumentException("Only shipped orders can be delivered.");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        Order saved = orderRepository.save(order);
        recordOrderMetric("deliver", "success", startNanos);
        logOrderAudit(order.getUserId(), adminUsername, AuditLog.AuditAction.ORDER_DELIVERED, saved);
        logger.info("event=order_delivered orderId={} admin={}", saved.getId(), adminUsername);
        return toResponse(saved);
    }

    private Order loadOrderWithItems(Long orderId) {
        return orderRepository.findByIdWithItemsAndProduct(orderId)
                .or(() -> orderRepository.findById(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private void preloadOrderItemsAndProducts(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        orderRepository.findAllWithItemsAndProductByIdIn(orderIds);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Long productId = item.getProduct().getId();
            inventoryService.increaseStockWithOptimisticLock(productId, item.getQuantity());
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
                .paymentProvider(order.getPaymentProvider())
                .paymentProviderStatus(order.getPaymentProviderStatus() != null ? order.getPaymentProviderStatus() : PaymentProviderStatus.NOT_STARTED)
                .paymentConversationId(order.getPaymentConversationId())
                .paymentReferenceId(order.getPaymentReferenceId())
                .paymentErrorMessage(order.getPaymentErrorMessage())
                .totalAmount(order.getTotalAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .couponCode(order.getCouponCode())
                .shippingMethod(order.getShippingMethod())
                .shippingFullName(order.getShippingFullName())
                .shippingEmail(order.getShippingEmail())
                .shippingPhone(order.getShippingPhone())
                .shippingAddressLine(order.getShippingAddressLine())
                .shippingCity(order.getShippingCity())
                .shippingPostalCode(order.getShippingPostalCode())
                .shippingCountry(order.getShippingCountry())
                .trackingNumber(order.getTrackingNumber())
                .cancelReason(order.getCancelReason())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paidAt(order.getPaidAt())
                .paymentInitializedAt(order.getPaymentInitializedAt())
                .paymentFailedAt(order.getPaymentFailedAt())
                .packedAt(order.getPackedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
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
        details.put("subtotalAmount", order.getSubtotalAmount());
        details.put("discountAmount", order.getDiscountAmount());
        details.put("shippingFee", order.getShippingFee());
        details.put("taxAmount", order.getTaxAmount());
        details.put("couponCode", order.getCouponCode());
        details.put("shippingMethod", order.getShippingMethod() != null ? order.getShippingMethod().name() : null);
        details.put("trackingNumber", order.getTrackingNumber());
        details.put("cancelReason", order.getCancelReason());
        details.put("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        details.put("paymentProvider", order.getPaymentProvider() != null ? order.getPaymentProvider().name() : null);
        details.put("paymentProviderStatus", order.getPaymentProviderStatus() != null ? order.getPaymentProviderStatus().name() : null);
        details.put("paymentConversationId", order.getPaymentConversationId());
        details.put("paymentReferenceId", order.getPaymentReferenceId());
        details.put("itemCount", order.getItems().size());
        auditService.logSystemEvent(userId, actorUsername, action, "Order event", details);
    }

    private void applyShippingAddress(Order order, ShippingAddressRequest shippingAddress) {
        if (shippingAddress == null) {
            return;
        }
        order.setShippingFullName(trimOrNull(shippingAddress.getFullName()));
        order.setShippingEmail(trimOrNull(shippingAddress.getEmail()));
        order.setShippingPhone(trimOrNull(shippingAddress.getPhone()));
        order.setShippingAddressLine(trimOrNull(shippingAddress.getAddressLine()));
        order.setShippingCity(trimOrNull(shippingAddress.getCity()));
        order.setShippingPostalCode(trimOrNull(shippingAddress.getPostalCode()));
        order.setShippingCountry(trimOrNull(shippingAddress.getCountry()));
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
