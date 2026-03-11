package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.CancelOrderRequest;
import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.example.ecommerce.common.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Public Orders", description = "Authenticated customer order operations")
public class OrderController {
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Create Order", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Order created", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = getCurrentUsername();
        return idempotencyService.findReplayResponse(username, "orders:create", idempotencyKey, request, OrderResponse.class)
                .orElseGet(() -> {
                    OrderResponse created = orderService.createOrder(username, request);
                    idempotencyService.saveResponse(username, "orders:create", idempotencyKey, request, 201, created);
                    return ResponseEntity.status(201).body(created);
                });
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Cancel My Order", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Order cancelled", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Order access denied", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> cancelMyOrder(@PathVariable Long id,
                                                       @Valid @RequestBody(required = false) CancelOrderRequest request,
                                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = getCurrentUsername();
        String cancelReason = request != null ? request.getReason() : null;
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("orderId", id);
        requestPayload.put("reason", cancelReason);
        return idempotencyService.findReplayResponse(username, "orders:cancel", idempotencyKey, requestPayload, OrderResponse.class)
                .orElseGet(() -> {
                    OrderResponse cancelled = orderService.cancelMyOrder(username, id, cancelReason);
                    idempotencyService.saveResponse(username, "orders:cancel", idempotencyKey, requestPayload, 200, cancelled);
                    return ResponseEntity.ok(cancelled);
                });
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "List My Orders", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(getCurrentUsername(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "List My Orders (Alias)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(getCurrentUsername(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "Get My Order", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> getMyOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrderById(getCurrentUsername(), id));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
