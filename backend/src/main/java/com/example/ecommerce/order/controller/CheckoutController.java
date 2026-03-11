package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.CheckoutQuoteRequest;
import com.example.ecommerce.order.dto.CheckoutQuoteResponse;
import com.example.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@Tag(name = "Checkout", description = "Checkout quote and pricing operations")
public class CheckoutController {
    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/quote")
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Get checkout quote", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CheckoutQuoteResponse> quote(@Valid @RequestBody CheckoutQuoteRequest request) {
        return ResponseEntity.ok(orderService.quoteCheckout(getCurrentUsername(), request));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
