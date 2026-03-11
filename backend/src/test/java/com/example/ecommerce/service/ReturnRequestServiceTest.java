package com.example.ecommerce.service;

import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.order.exception.OrderAccessDeniedException;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.ReturnRequest;
import com.example.ecommerce.order.model.ReturnRequestStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.repository.ReturnRequestRepository;
import com.example.ecommerce.order.service.ReturnRequestService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {
    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuditService auditService;

    private ReturnRequestService returnRequestService;

    @BeforeEach
    void setUp() {
        returnRequestService = new ReturnRequestService(
                returnRequestRepository,
                orderRepository,
                auditService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void createMyReturnRequest_ShouldCreateWhenDeliveredAndOwned() {
        Order order = new Order();
        order.setId(15L);
        order.setUserId(2L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(returnRequestRepository.findByOrder_Id(15L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest request = invocation.getArgument(0);
            request.setId(99L);
            return request;
        });

        var response = returnRequestService.createMyReturnRequest("alice", 15L, "Defolu ürün geldi");

        assertEquals(99L, response.getId());
        assertEquals(ReturnRequestStatus.REQUESTED, response.getStatus());
        assertEquals(15L, response.getOrderId());
        assertEquals("Defolu ürün geldi", response.getReason());
        verify(returnRequestRepository).save(any(ReturnRequest.class));
    }

    @Test
    void createMyReturnRequest_ShouldThrowWhenOrderBelongsToDifferentUser() {
        Order order = new Order();
        order.setId(16L);
        order.setUsername("other");
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(16L)).thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class, () -> returnRequestService.createMyReturnRequest("alice", 16L, "Wrong item"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void createMyReturnRequest_ShouldThrowWhenOrderIsNotDelivered() {
        Order order = new Order();
        order.setId(17L);
        order.setUsername("alice");
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(17L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> returnRequestService.createMyReturnRequest("alice", 17L, "Wrong size"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void approveReturnRequest_ShouldSetApproved() {
        Order order = new Order();
        order.setId(18L);
        order.setUserId(2L);
        order.setUsername("alice");

        ReturnRequest request = new ReturnRequest();
        request.setId(7L);
        request.setOrder(order);
        request.setUserId(2L);
        request.setUsername("alice");
        request.setStatus(ReturnRequestStatus.REQUESTED);
        request.setReason("Defective");

        when(returnRequestRepository.findById(7L)).thenReturn(Optional.of(request));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = returnRequestService.approveReturnRequest(7L, "admin");

        assertEquals(ReturnRequestStatus.APPROVED, response.getStatus());
        assertEquals("admin", response.getReviewedBy());
    }

    @Test
    void rejectReturnRequest_ShouldSetRejected() {
        Order order = new Order();
        order.setId(19L);
        order.setUserId(2L);
        order.setUsername("alice");

        ReturnRequest request = new ReturnRequest();
        request.setId(8L);
        request.setOrder(order);
        request.setUserId(2L);
        request.setUsername("alice");
        request.setStatus(ReturnRequestStatus.REQUESTED);
        request.setReason("Not as described");

        when(returnRequestRepository.findById(8L)).thenReturn(Optional.of(request));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = returnRequestService.rejectReturnRequest(8L, "admin", "Used item traces");

        assertEquals(ReturnRequestStatus.REJECTED, response.getStatus());
        assertEquals("Used item traces", response.getAdminNote());
    }
}
