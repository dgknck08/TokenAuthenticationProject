package com.example.ecommerce.payment.iyzico;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.exception.OrderNotFoundException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentMethod;
import com.example.ecommerce.order.model.PaymentProvider;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.CheckoutPricingService;
import com.example.ecommerce.payment.iyzico.dto.IyzicoInitPaymentResponse;
import com.example.ecommerce.payment.iyzico.dto.IyzicoPaymentCallbackResponse;
import com.example.ecommerce.payment.iyzico.dto.IyzicoPaymentStatusResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class IyzicoPaymentService {
    private static final Logger logger = LoggerFactory.getLogger(IyzicoPaymentService.class);

    private final IyzicoProperties properties;
    private final IyzicoGatewayClient gatewayClient;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CheckoutPricingService checkoutPricingService;
    private final CartService cartService;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public IyzicoPaymentService(IyzicoProperties properties,
                                IyzicoGatewayClient gatewayClient,
                                OrderRepository orderRepository,
                                UserRepository userRepository,
                                CheckoutPricingService checkoutPricingService,
                                CartService cartService,
                                AuditService auditService,
                                MeterRegistry meterRegistry) {
        this.properties = properties;
        this.gatewayClient = gatewayClient;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.checkoutPricingService = checkoutPricingService;
        this.cartService = cartService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    public IyzicoInitPaymentResponse initializePayment(String username, Long orderId, String locale) {
        long startNanos = System.nanoTime();
        ensureIyzicoConfigured();

        Order order = loadOwnedOrder(username, orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            recordMetric("init", "failed", startNanos);
            throw new IllegalArgumentException("Only CREATED orders can start card payment.");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            recordMetric("init", "failed", startNanos);
            throw new IllegalArgumentException("Order has no items to pay.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        ensureOrderPaymentDataReady(order, user);

        String conversationId = "order-" + order.getId() + "-" + UUID.randomUUID();
        String callbackUrl = buildCallbackUrl(normalizeLocale(locale));
        IyzicoInitializeResult providerResult = gatewayClient.initializeCheckoutForm(
                order, user, normalizeLocale(locale), conversationId, callbackUrl
        );

        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentProvider(PaymentProvider.IYZICO);
        order.setPaymentConversationId(providerResult.conversationId() != null
                ? providerResult.conversationId()
                : conversationId);
        order.setPaymentToken(providerResult.token());
        order.setPaymentInitializedAt(Instant.now());

        if (providerResult.success()) {
            order.setPaymentProviderStatus(PaymentProviderStatus.PENDING);
            order.setPaymentErrorMessage(null);
            order.setPaymentFailedAt(null);
            recordMetric("init", "success", startNanos);
            auditService.logSystemEvent(
                    order.getUserId(),
                    username,
                    AuditLog.AuditAction.ORDER_PAYMENT_INITIATED,
                    "Iyzico payment initialized",
                    details(
                            "orderId", order.getId(),
                            "conversationId", order.getPaymentConversationId(),
                            "provider", PaymentProvider.IYZICO.name()
                    )
            );
        } else {
            order.setPaymentProviderStatus(PaymentProviderStatus.FAILED);
            order.setPaymentFailedAt(Instant.now());
            order.setPaymentErrorMessage(firstNonBlank(providerResult.errorMessage(), "Iyzico init failed"));
            recordMetric("init", "failed", startNanos);
            auditService.logSystemEvent(
                    order.getUserId(),
                    username,
                    AuditLog.AuditAction.ORDER_PAYMENT_FAILED,
                    "Iyzico payment initialization failed",
                    details(
                            "orderId", order.getId(),
                            "conversationId", order.getPaymentConversationId(),
                            "errorCode", providerResult.errorCode(),
                            "errorMessage", providerResult.errorMessage()
                    )
            );
        }
        orderRepository.save(order);

        return IyzicoInitPaymentResponse.builder()
                .paymentStatus(order.getPaymentProviderStatus())
                .paymentPageUrl(providerResult.paymentPageUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public IyzicoPaymentStatusResponse getPaymentStatus(String username, Long orderId) {
        Order order = loadOwnedOrder(username, orderId);
        return toPaymentStatus(order);
    }

    public IyzicoPaymentCallbackResponse handleCallback(String token,
                                                        String conversationIdHint) {
        long startNanos = System.nanoTime();
        ensureIyzicoConfigured();
        if (token == null || token.isBlank()) {
            recordMetric("callback", "failed", startNanos);
            throw new IllegalArgumentException("Missing payment token.");
        }

        IyzicoRetrieveResult retrieveResult = gatewayClient.retrieveCheckoutForm(token.trim());
        Order order = resolveOrderForCallback(retrieveResult, conversationIdHint, token);

        auditService.logSystemEvent(
                order.getUserId(),
                order.getUsername(),
                AuditLog.AuditAction.ORDER_PAYMENT_CALLBACK_RECEIVED,
                "Iyzico callback received",
                details(
                        "orderId", order.getId(),
                        "conversationId", firstNonBlank(retrieveResult.conversationId(), order.getPaymentConversationId()),
                        "status", retrieveResult.status(),
                        "paymentStatus", retrieveResult.paymentStatus()
                )
        );

        if (retrieveResult.success()) {
            boolean applied = applySuccessfulPayment(order, firstNonBlank(retrieveResult.conversationId(), conversationIdHint),
                    retrieveResult.paymentId(), token);
            if (!applied) {
                recordMetric("callback", "ignored", startNanos);
                return IyzicoPaymentCallbackResponse.builder()
                        .orderId(order.getId())
                        .conversationId(order.getPaymentConversationId())
                        .paymentReferenceId(order.getPaymentReferenceId())
                        .paymentStatus(order.getPaymentProviderStatus())
                        .orderStatus(order.getStatus())
                        .success(false)
                        .message("Payment confirmation ignored for cancelled order.")
                        .build();
            }
            recordMetric("callback", "success", startNanos);
            return IyzicoPaymentCallbackResponse.builder()
                    .orderId(order.getId())
                    .conversationId(order.getPaymentConversationId())
                    .paymentReferenceId(order.getPaymentReferenceId())
                    .paymentStatus(order.getPaymentProviderStatus())
                    .orderStatus(order.getStatus())
                    .success(true)
                    .message("Payment confirmed.")
                    .build();
        }

        applyFailedPayment(order, firstNonBlank(retrieveResult.conversationId(), conversationIdHint), token,
                firstNonBlank(retrieveResult.errorMessage(), "Iyzico payment failed"));
        recordMetric("callback", "failed", startNanos);
        return IyzicoPaymentCallbackResponse.builder()
                .orderId(order.getId())
                .conversationId(order.getPaymentConversationId())
                .paymentReferenceId(order.getPaymentReferenceId())
                .paymentStatus(order.getPaymentProviderStatus())
                .orderStatus(order.getStatus())
                .success(false)
                .message(firstNonBlank(retrieveResult.errorMessage(), "Payment failed."))
                .build();
    }

    public void processWebhookNotification(String eventType,
                                           String conversationId,
                                           String paymentId,
                                           String paymentStatus,
                                           String errorMessage) {
        long startNanos = System.nanoTime();
        if (conversationId == null || conversationId.isBlank()) {
            recordMetric("webhook", "ignored", startNanos);
            return;
        }

        Order order = orderRepository.findByPaymentConversationId(conversationId.trim())
                .orElse(null);
        if (order == null) {
            recordMetric("webhook", "ignored", startNanos);
            return;
        }

        auditService.logSystemEvent(
                order.getUserId(),
                order.getUsername(),
                AuditLog.AuditAction.ORDER_PAYMENT_WEBHOOK_RECEIVED,
                "Iyzico webhook received",
                details(
                        "orderId", order.getId(),
                        "conversationId", conversationId,
                        "eventType", eventType,
                        "paymentStatus", paymentStatus,
                        "paymentId", paymentId
                )
        );

        if (isSuccessfulWebhook(eventType, paymentStatus)) {
            boolean applied = applySuccessfulPayment(order, conversationId, paymentId, order.getPaymentToken());
            recordMetric("webhook", applied ? "success" : "ignored", startNanos);
            return;
        }

        if (isFailedWebhook(eventType, paymentStatus)) {
            if (order.getPaymentProviderStatus() == PaymentProviderStatus.SUCCESS || order.getStatus() == OrderStatus.PAID) {
                recordMetric("webhook", "ignored", startNanos);
                return;
            }
            applyFailedPayment(order, conversationId, order.getPaymentToken(),
                    firstNonBlank(errorMessage, "Iyzico webhook reported payment failure"));
            recordMetric("webhook", "failed", startNanos);
            return;
        }

        recordMetric("webhook", "ignored", startNanos);
    }

    private boolean applySuccessfulPayment(Order order, String conversationId, String paymentId, String token) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return false;
        }
        boolean wasPaid = order.getStatus() == OrderStatus.PAID
                && order.getPaymentProviderStatus() == PaymentProviderStatus.SUCCESS;
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentProvider(PaymentProvider.IYZICO);
        order.setPaymentProviderStatus(PaymentProviderStatus.SUCCESS);
        order.setPaymentConversationId(firstNonBlank(conversationId, order.getPaymentConversationId()));
        order.setPaymentReferenceId(firstNonBlank(paymentId, order.getPaymentReferenceId()));
        order.setPaymentToken(firstNonBlank(token, order.getPaymentToken()));
        order.setPaymentErrorMessage(null);
        order.setPaymentFailedAt(null);
        boolean firstPaymentTransition = false;
        if (order.getStatus() == OrderStatus.CREATED) {
            firstPaymentTransition = true;
            order.setStatus(OrderStatus.PAID);
            if (order.getPaidAt() == null) {
                order.setPaidAt(Instant.now());
            }
        }
        orderRepository.save(order);
        if (firstPaymentTransition) {
            checkoutPricingService.recordCouponRedemptionByCode(order.getCouponCode(), order.getUserId(), order.getId());
            try {
                cartService.clearCart(order.getUserId());
            } catch (Exception ex) {
                logger.warn("Failed to clear cart after payment success for userId={} orderId={}",
                        order.getUserId(), order.getId(), ex);
            }
        }
        if (wasPaid) {
            return true;
        }
        auditService.logSystemEvent(
                order.getUserId(),
                order.getUsername(),
                AuditLog.AuditAction.ORDER_PAID,
                "Order paid by Iyzico",
                details(
                        "orderId", order.getId(),
                        "conversationId", order.getPaymentConversationId(),
                        "paymentReferenceId", order.getPaymentReferenceId()
                )
        );
        return true;
    }

    private void applyFailedPayment(Order order, String conversationId, String token, String errorMessage) {
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentProvider(PaymentProvider.IYZICO);
        order.setPaymentProviderStatus(PaymentProviderStatus.FAILED);
        order.setPaymentConversationId(firstNonBlank(conversationId, order.getPaymentConversationId()));
        order.setPaymentToken(firstNonBlank(token, order.getPaymentToken()));
        order.setPaymentFailedAt(Instant.now());
        order.setPaymentErrorMessage(firstNonBlank(errorMessage, "Iyzico payment failed"));
        orderRepository.save(order);
        auditService.logSystemEvent(
                order.getUserId(),
                order.getUsername(),
                AuditLog.AuditAction.ORDER_PAYMENT_FAILED,
                "Order payment failed by Iyzico",
                details(
                        "orderId", order.getId(),
                        "conversationId", order.getPaymentConversationId(),
                        "errorMessage", order.getPaymentErrorMessage()
                )
        );
    }

    private Order resolveOrderForCallback(IyzicoRetrieveResult retrieveResult,
                                          String conversationIdHint,
                                          String token) {
        Order tokenOrder = orderRepository.findByPaymentToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for payment token."));
        String providerConversationId = trimToNull(retrieveResult.conversationId());
        if (providerConversationId != null) {
            String orderConversationId = trimToNull(tokenOrder.getPaymentConversationId());
            if (orderConversationId != null && !orderConversationId.equals(providerConversationId)) {
                throw new IllegalArgumentException("Payment conversation id mismatch.");
            }
        }

        String hintedConversationId = trimToNull(conversationIdHint);
        if (hintedConversationId != null) {
            String orderConversationId = trimToNull(tokenOrder.getPaymentConversationId());
            if (orderConversationId == null || !orderConversationId.equals(hintedConversationId)) {
                throw new IllegalArgumentException("Callback conversation id does not match token owner order.");
            }
        }

        return tokenOrder;
    }

    private Order loadOwnedOrder(String username, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (!order.getUsername().equals(username)) {
            throw new OrderAccessDeniedException("Order does not belong to current user.");
        }
        return order;
    }

    private IyzicoPaymentStatusResponse toPaymentStatus(Order order) {
        return IyzicoPaymentStatusResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .provider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentProviderStatus() != null
                        ? order.getPaymentProviderStatus()
                        : PaymentProviderStatus.NOT_STARTED)
                .conversationId(order.getPaymentConversationId())
                .paymentReferenceId(order.getPaymentReferenceId())
                .paymentErrorMessage(order.getPaymentErrorMessage())
                .paymentInitializedAt(order.getPaymentInitializedAt())
                .paidAt(order.getPaidAt())
                .paymentFailedAt(order.getPaymentFailedAt())
                .build();
    }

    private String buildCallbackUrl(String locale) {
        return UriComponentsBuilder.fromUriString(properties.getCallbackUrl())
                .queryParam("locale", locale)
                .build()
                .toUriString();
    }

    private boolean isSuccessfulWebhook(String eventType, String paymentStatus) {
        String normalizedEventType = normalize(eventType);
        String normalizedPaymentStatus = normalize(paymentStatus);
        return "SUCCESS".equals(normalizedPaymentStatus)
                || "SUCCEEDED".equals(normalizedPaymentStatus)
                || "PAYMENT_SUCCEEDED".equals(normalizedEventType)
                || "PAYMENT_SUCCESS".equals(normalizedEventType)
                || "CHECKOUTFORM_AUTH".equals(normalizedEventType)
                || "CHECKOUT_FORM_AUTH".equals(normalizedEventType);
    }

    private boolean isFailedWebhook(String eventType, String paymentStatus) {
        String normalizedEventType = normalize(eventType);
        String normalizedPaymentStatus = normalize(paymentStatus);
        return "FAILURE".equals(normalizedPaymentStatus)
                || "FAILED".equals(normalizedPaymentStatus)
                || "PAYMENT_FAILED".equals(normalizedEventType)
                || "PAYMENT_FAILURE".equals(normalizedEventType);
    }

    private String normalizeLocale(String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return "en";
        }
        return "tr";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private void ensureIyzicoConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Iyzico payment is disabled.");
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getSecretKey()) || isBlank(properties.getApiBaseUrl())) {
            throw new IllegalStateException("Iyzico payment credentials are missing.");
        }
        if (isBlank(properties.getCallbackUrl())) {
            throw new IllegalStateException("Iyzico callback URL is not configured.");
        }
        if (isBlank(properties.getDefaultIdentityNumber())) {
            throw new IllegalStateException("Iyzico default identity number is not configured.");
        }
        if (!properties.getDefaultIdentityNumber().trim().matches("\\d{11}")) {
            throw new IllegalStateException("Iyzico default identity number must be 11 digits.");
        }
        if (isBlank(properties.getDefaultBuyerIp())) {
            throw new IllegalStateException("Iyzico default buyer ip is not configured.");
        }
        if (!isSandboxBaseUrl(properties.getApiBaseUrl())) {
            if ("11111111111".equals(properties.getDefaultIdentityNumber().trim())) {
                throw new IllegalStateException("Iyzico default identity number cannot be a sandbox placeholder in production.");
            }
            if (isLoopbackIp(properties.getDefaultBuyerIp())) {
                throw new IllegalStateException("Iyzico buyer ip cannot be a loopback address in production.");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isSandboxBaseUrl(String apiBaseUrl) {
        if (apiBaseUrl == null) {
            return false;
        }
        return apiBaseUrl.toLowerCase().contains("sandbox");
    }

    private boolean isLoopbackIp(String ip) {
        String normalized = trimToNull(ip);
        if (normalized == null) {
            return false;
        }
        return "127.0.0.1".equals(normalized) || "::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    private void ensureOrderPaymentDataReady(Order order, User user) {
        List<String> missingFields = new ArrayList<>();
        requireField(order.getShippingFullName(), "shippingFullName", missingFields);
        requireField(order.getShippingEmail(), "shippingEmail", missingFields);
        requireField(order.getShippingPhone(), "shippingPhone", missingFields);
        requireField(order.getShippingAddressLine(), "shippingAddressLine", missingFields);
        requireField(order.getShippingCity(), "shippingCity", missingFields);
        requireField(order.getShippingPostalCode(), "shippingPostalCode", missingFields);
        requireField(order.getShippingCountry(), "shippingCountry", missingFields);
        requireField(user.getEmail(), "userEmail", missingFields);
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            missingFields.add("totalAmount");
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Order is not ready for card payment. Missing/invalid fields: "
                    + String.join(", ", missingFields));
        }
    }

    private void requireField(String value, String fieldName, List<String> missingFields) {
        if (trimToNull(value) == null) {
            missingFields.add(fieldName);
        }
    }

    private String firstNonBlank(String value, String fallback) {
        String primary = trimToNull(value);
        if (primary != null) {
            return primary;
        }
        return trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> details(Object... keyValues) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null) {
                details.put(String.valueOf(key), value);
            }
        }
        return details;
    }

    private void recordMetric(String action, String outcome, long startNanos) {
        meterRegistry.counter("ecommerce.payment.events", "provider", "iyzico", "action", action, "outcome", outcome).increment();
        Timer.builder("ecommerce.payment.action.duration")
                .tag("provider", "iyzico")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
