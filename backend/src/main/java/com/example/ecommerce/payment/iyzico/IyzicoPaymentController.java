package com.example.ecommerce.payment.iyzico;

import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.example.ecommerce.payment.iyzico.dto.IyzicoInitPaymentRequest;
import com.example.ecommerce.payment.iyzico.dto.IyzicoInitPaymentResponse;
import com.example.ecommerce.payment.iyzico.dto.IyzicoPaymentCallbackResponse;
import com.example.ecommerce.payment.iyzico.dto.IyzicoPaymentStatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/iyzico")
@Tag(name = "Iyzico Payments", description = "Iyzico card payment operations")
public class IyzicoPaymentController {
    private final IyzicoPaymentService paymentService;
    private final IyzicoProperties properties;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IyzicoPaymentController(IyzicoPaymentService paymentService,
                                   IyzicoProperties properties,
                                   IdempotencyService idempotencyService,
                                   ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.properties = properties;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/orders/{orderId}/init")
    @PreAuthorize("hasAuthority('ORDER_WRITE')")
    @Operation(summary = "Initialize Iyzico card payment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IyzicoInitPaymentResponse> initializePayment(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) IyzicoInitPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String username = getCurrentUsername();
        IyzicoInitPaymentRequest effectiveRequest = request != null ? request : new IyzicoInitPaymentRequest();
        String operation = "iyzico:init:" + orderId;
        return idempotencyService.findReplayResponse(
                        username,
                        operation,
                        idempotencyKey,
                        effectiveRequest,
                        IyzicoInitPaymentResponse.class
                )
                .orElseGet(() -> {
                    IyzicoInitPaymentResponse response = paymentService.initializePayment(
                            username,
                            orderId,
                            effectiveRequest.getLocale()
                    );
                    idempotencyService.saveResponse(username, operation, idempotencyKey, effectiveRequest, 200, response);
                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAuthority('ORDER_READ')")
    @Operation(summary = "Get Iyzico payment status for order", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IyzicoPaymentStatusResponse> getPaymentStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(getCurrentUsername(), orderId));
    }

    @RequestMapping(value = "/callback", method = {RequestMethod.POST, RequestMethod.GET})
    @Operation(summary = "Iyzico callback endpoint", description = "Public callback endpoint called by Iyzico after card payment.")
    public ResponseEntity<?> handleCallback(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "locale", required = false, defaultValue = "tr") String locale,
            @RequestParam(value = "redirect", required = false, defaultValue = "true") boolean redirectToFrontend,
            @RequestBody(required = false) String payload) {
        String resolvedToken = resolveToken(token, payload);
        IyzicoPaymentCallbackResponse response = paymentService.handleCallback(resolvedToken, conversationId);

        if (!redirectToFrontend || properties.getFrontendBaseUrl() == null || properties.getFrontendBaseUrl().isBlank()) {
            return ResponseEntity.ok(response);
        }

        String normalizedLocale = "en".equalsIgnoreCase(locale) ? "en" : "tr";
        URI redirectUrl = UriComponentsBuilder.fromUriString(properties.getFrontendBaseUrl())
                .pathSegment(normalizedLocale, "account", "orders", String.valueOf(response.getOrderId()))
                .queryParam("payment", response.isSuccess() ? "success" : "failed")
                .build()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl.toString())
                .body(Map.of("redirect", redirectUrl.toString(), "success", response.isSuccess()));
    }

    private String resolveToken(String token, String payload) {
        if (token != null && !token.isBlank()) {
            return token;
        }
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode tokenNode = root.path("token");
            return tokenNode.isTextual() ? tokenNode.asText() : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
