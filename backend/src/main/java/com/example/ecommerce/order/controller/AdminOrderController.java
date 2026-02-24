package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.api.ApiErrorResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin Orders", description = "Administrative order operations")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List All Orders", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin(pageable));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel Order", description = "Cancels a CREATED order and restores stock", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Order cancelled", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderForAdmin(id, getCurrentUsername()));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund Order", description = "Refunds a PAID order and restores stock", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Order refunded", content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OrderResponse> refundOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.refundOrderForAdmin(id, getCurrentUsername()));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "admin-system";
        }
        return authentication.getName();
    }
}
