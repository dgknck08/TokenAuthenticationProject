package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.api.ApiErrorResponse;
import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.example.ecommerce.order.dto.CreateReturnRequest;
import com.example.ecommerce.order.dto.ReturnRequestResponse;
import com.example.ecommerce.order.service.ReturnRequestService;
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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Return Requests", description = "Customer return request operations")
public class ReturnRequestController {
    private final ReturnRequestService returnRequestService;
    private final IdempotencyService idempotencyService;

    public ReturnRequestController(ReturnRequestService returnRequestService, IdempotencyService idempotencyService) {
        this.returnRequestService = returnRequestService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/{id}/return-requests")
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Create Return Request", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Return request created", content = @Content(schema = @Schema(implementation = ReturnRequestResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state or payload", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Order access denied", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReturnRequestResponse> createReturnRequest(@PathVariable Long id,
                                                                     @Valid @RequestBody CreateReturnRequest request,
                                                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = getCurrentUsername();
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("orderId", id);
        requestPayload.put("reason", request.getReason());
        return idempotencyService.findReplayResponse(username, "orders:return:create", idempotencyKey, requestPayload, ReturnRequestResponse.class)
                .orElseGet(() -> {
                    ReturnRequestResponse created = returnRequestService.createMyReturnRequest(username, id, request.getReason());
                    idempotencyService.saveResponse(username, "orders:return:create", idempotencyKey, requestPayload, 201, created);
                    return ResponseEntity.status(201).body(created);
                });
    }

    @GetMapping("/return-requests")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "List My Return Requests", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<ReturnRequestResponse>> getMyReturnRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(returnRequestService.getMyReturnRequests(getCurrentUsername(), pageable));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
