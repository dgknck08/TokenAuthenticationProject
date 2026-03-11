package com.example.ecommerce.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.ShippingMethod;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.CheckoutPricingService;
import com.example.ecommerce.order.service.OrderPricingItem;
import com.example.ecommerce.order.service.OrderPricingResult;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private CheckoutPricingService checkoutPricingService;
    @Mock
    private AuditService auditService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                userRepository,
                productRepository,
                inventoryService,
                checkoutPricingService,
                auditService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void createOrder_ShouldCreateOrderAndDecreaseStock() {
        User user = User.builder()
                .id(2L)
                .username("alice")
                .password("pw")
                .email("alice@test.com")
                .firstName("A")
                .lastName("L")
                .build();
        Product product = new Product(5L, "Guitar", "Desc", new BigDecimal("1000.00"), "img", "Strings", 10);

        CreateOrderRequest request = new CreateOrderRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(5L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(checkoutPricingService.buildPricing(any(), any(), any(), any()))
                .thenReturn(new OrderPricingResult(
                        List.of(new OrderPricingItem(product, 2)),
                        new BigDecimal("2000.00"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("2000.00"),
                        ShippingMethod.STANDARD,
                        null,
                        null
                ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        var response = orderService.createOrder("alice", request);

        assertEquals(99L, response.getId());
        assertEquals(OrderStatus.CREATED, response.getStatus());
        assertEquals(new BigDecimal("2000.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        verify(inventoryService).decreaseStockWithOptimisticLock(5L, 2);
    }

    @Test
    void getMyOrderById_ShouldThrowWhenOrderBelongsToDifferentUser() {
        Order order = new Order();
        order.setId(3L);
        order.setUsername("other");
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(BigDecimal.TEN);
        order.setItems(List.of(new OrderItem()));

        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class, () -> orderService.getMyOrderById("alice", 3L));
    }

    @Test
    void cancelOrderForAdmin_ShouldSetCancelledAndRestoreStock() {
        Product product = new Product();
        product.setId(6L);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(22L);
        order.setStatus(OrderStatus.CREATED);
        order.setItems(List.of(item));

        when(orderRepository.findById(22L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.cancelOrderForAdmin(22L, "admin");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryService).increaseStockWithOptimisticLock(6L, 2);
    }

    @Test
    void cancelMyOrder_ShouldSetCancelledAndRestoreStock() {
        Product product = new Product();
        product.setId(6L);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(22L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.CREATED);
        order.setItems(List.of(item));

        when(orderRepository.findById(22L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.cancelMyOrder("alice", 22L);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryService).increaseStockWithOptimisticLock(6L, 2);
    }

    @Test
    void cancelMyOrder_ShouldStoreCancelReason() {
        Order order = new Order();
        order.setId(25L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.CREATED);
        order.setItems(List.of());

        when(orderRepository.findById(25L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.cancelMyOrder("alice", 25L, "Yanlis urun secimi");

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        assertEquals("Yanlis urun secimi", response.getCancelReason());
    }

    @Test
    void cancelMyOrder_ShouldThrowWhenOrderStatusIsNotCreated() {
        Order order = new Order();
        order.setId(23L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.PAID);
        order.setItems(List.of());

        when(orderRepository.findById(23L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelMyOrder("alice", 23L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelMyOrder_ShouldThrowWhenOrderBelongsToDifferentUser() {
        Order order = new Order();
        order.setId(24L);
        order.setUsername("other");
        order.setStatus(OrderStatus.CREATED);
        order.setItems(List.of());

        when(orderRepository.findById(24L)).thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class, () -> orderService.cancelMyOrder("alice", 24L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void refundOrderForAdmin_ShouldSetRefundedAndRestoreStock() {
        Product product = new Product();
        product.setId(9L);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);

        Order order = new Order();
        order.setId(30L);
        order.setStatus(OrderStatus.PAID);
        order.setItems(List.of(item));

        when(orderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.refundOrderForAdmin(30L, "admin");

        assertEquals(OrderStatus.REFUNDED, response.getStatus());
        verify(inventoryService).increaseStockWithOptimisticLock(9L, 1);
    }
}
