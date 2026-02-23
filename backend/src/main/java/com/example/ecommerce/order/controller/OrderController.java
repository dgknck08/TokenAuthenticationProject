package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.dto.PayOrderRequest;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.common.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Public Orders", description = "Authenticated customer order operations")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Create Order", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Order created", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(201).body(orderService.createOrder(getCurrentUsername(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "List My Orders", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders(getCurrentUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "List My Orders (Alias)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<OrderResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getMyOrders(getCurrentUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "Get My Order", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> getMyOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrderById(getCurrentUsername(), id));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Pay My Order (Simulation)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Order paid", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> payMyOrder(@PathVariable Long id, @Valid @RequestBody PayOrderRequest request) {
        return ResponseEntity.ok(orderService.payMyOrder(getCurrentUsername(), id, request));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
