package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.api.ApiErrorResponse;
import com.example.ecommerce.order.dto.RejectReturnRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/return-requests")
@Tag(name = "Admin Return Requests", description = "Administrative return request operations")
public class AdminReturnRequestController {
    private final ReturnRequestService returnRequestService;

    public AdminReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List All Return Requests", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<ReturnRequestResponse>> getAllReturnRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(returnRequestService.getAllReturnRequestsForAdmin(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve Return Request", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Return request approved", content = @Content(schema = @Schema(implementation = ReturnRequestResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Return request not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReturnRequestResponse> approveReturnRequest(@PathVariable Long id) {
        return ResponseEntity.ok(returnRequestService.approveReturnRequest(id, getCurrentUsername()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject Return Request", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Return request rejected", content = @Content(schema = @Schema(implementation = ReturnRequestResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Return request not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReturnRequestResponse> rejectReturnRequest(@PathVariable Long id,
                                                                     @Valid @RequestBody RejectReturnRequest request) {
        return ResponseEntity.ok(returnRequestService.rejectReturnRequest(id, getCurrentUsername(), request.getNote()));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "admin-system";
        }
        return authentication.getName();
    }
}
