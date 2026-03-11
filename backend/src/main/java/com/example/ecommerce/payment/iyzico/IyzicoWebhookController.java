package com.example.ecommerce.payment.iyzico;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/iyzico")
public class IyzicoWebhookController {
    private final IyzicoWebhookService webhookService;

    public IyzicoWebhookController(IyzicoWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody(required = false) String payload,
            @RequestHeader(value = "x-iyzi-signature-v3", required = false) String signature) {
        String safePayload = payload == null ? "" : payload;
        webhookService.validateAndLogEvent(safePayload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
