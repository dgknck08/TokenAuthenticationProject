package com.example.ecommerce.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.CheckoutPricingService;
import com.example.ecommerce.payment.iyzico.IyzicoGatewayClient;
import com.example.ecommerce.payment.iyzico.IyzicoInitializeResult;
import com.example.ecommerce.payment.iyzico.IyzicoPaymentService;
import com.example.ecommerce.payment.iyzico.IyzicoProperties;
import com.example.ecommerce.payment.iyzico.IyzicoRetrieveResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IyzicoPaymentServiceTest {
    @Mock
    private IyzicoGatewayClient gatewayClient;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CheckoutPricingService checkoutPricingService;
    @Mock
    private CartService cartService;
    @Mock
    private AuditService auditService;

    private IyzicoPaymentService paymentService;

    @BeforeEach
    void setUp() {
        IyzicoProperties properties = new IyzicoProperties();
        properties.setEnabled(true);
        properties.setApiBaseUrl("https://sandbox-api.iyzipay.com");
        properties.setApiKey("api-key");
        properties.setSecretKey("secret-key");
        properties.setDefaultIdentityNumber("11111111111");
        properties.setDefaultBuyerIp("10.0.0.25");
        properties.setCallbackUrl("http://localhost:8080/api/payments/iyzico/callback");
        paymentService = new IyzicoPaymentService(
                properties,
                gatewayClient,
                orderRepository,
                userRepository,
                checkoutPricingService,
                cartService,
                auditService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void initializePayment_shouldSetPendingWhenProviderReturnsSuccess() {
        Order order = buildCreatedOrder();
        User user = User.builder()
                .id(10L)
                .username("alice")
                .email("alice@test.com")
                .firstName("Alice")
                .lastName("Doe")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(gatewayClient.initializeCheckoutForm(any(), any(), any(), any(), any()))
                .thenReturn(new IyzicoInitializeResult(
                        true,
                        "success",
                        "conv-1",
                        "token-1",
                        "https://sandbox-iyzico/pay",
                        "<form/>",
                        1800L,
                        null,
                        null
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.initializePayment("alice", 1L, "tr");

        assertEquals(PaymentProviderStatus.PENDING, response.getPaymentStatus());
        assertEquals("https://sandbox-iyzico/pay", response.getPaymentPageUrl());

        ArgumentCaptor<String> callbackUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(gatewayClient).initializeCheckoutForm(any(), any(), any(), any(), callbackUrlCaptor.capture());
        assertEquals("http://localhost:8080/api/payments/iyzico/callback?locale=tr", callbackUrlCaptor.getValue());
    }

    @Test
    void callback_shouldMarkOrderPaidWhenRetrieveIsSuccessful() {
        Order order = buildCreatedOrder();
        order.setPaymentConversationId("conv-2");
        order.setPaymentToken("token-2");

        when(orderRepository.findByPaymentToken("token-2")).thenReturn(Optional.of(order));
        when(gatewayClient.retrieveCheckoutForm("token-2"))
                .thenReturn(new IyzicoRetrieveResult(
                        true,
                        "success",
                        "SUCCESS",
                        "conv-2",
                        "payment-123",
                        null,
                        null
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.handleCallback("token-2", "conv-2");

        assertEquals(true, response.isSuccess());
        assertEquals(OrderStatus.PAID, response.getOrderStatus());
        assertEquals(PaymentProviderStatus.SUCCESS, response.getPaymentStatus());
        assertEquals("payment-123", response.getPaymentReferenceId());
        verify(cartService).clearCart(10L);
    }

    @Test
    void initializePayment_shouldRejectSandboxIdentityOnProductionBaseUrl() {
        IyzicoProperties properties = new IyzicoProperties();
        properties.setEnabled(true);
        properties.setApiBaseUrl("https://api.iyzipay.com");
        properties.setApiKey("api-key");
        properties.setSecretKey("secret-key");
        properties.setDefaultIdentityNumber("11111111111");
        properties.setDefaultBuyerIp("10.0.0.25");
        properties.setCallbackUrl("http://localhost:8080/api/payments/iyzico/callback");

        IyzicoPaymentService productionGuardService = new IyzicoPaymentService(
                properties,
                gatewayClient,
                orderRepository,
                userRepository,
                checkoutPricingService,
                cartService,
                auditService,
                new SimpleMeterRegistry()
        );

        assertThrows(IllegalStateException.class, () -> productionGuardService.initializePayment("alice", 1L, "tr"));
    }

    @Test
    void callback_shouldIgnoreSuccessfulProviderResultForCancelledOrder() {
        Order order = buildCreatedOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentConversationId("conv-cancelled");
        order.setPaymentToken("token-cancelled");

        when(orderRepository.findByPaymentToken("token-cancelled")).thenReturn(Optional.of(order));
        when(gatewayClient.retrieveCheckoutForm("token-cancelled"))
                .thenReturn(new IyzicoRetrieveResult(
                        true,
                        "success",
                        "SUCCESS",
                        "conv-cancelled",
                        "payment-999",
                        null,
                        null
                ));

        var response = paymentService.handleCallback("token-cancelled", "conv-cancelled");

        assertEquals(false, response.isSuccess());
        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
    }

    private Order buildCreatedOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(10L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setShippingFullName("Alice Doe");
        order.setShippingEmail("alice@test.com");
        order.setShippingPhone("+905551112233");
        order.setShippingAddressLine("Kadikoy");
        order.setShippingCity("Istanbul");
        order.setShippingPostalCode("34710");
        order.setShippingCountry("Turkey");
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setProductNameSnapshot("Product");
        order.setItems(List.of(item));
        return order;
    }
}
