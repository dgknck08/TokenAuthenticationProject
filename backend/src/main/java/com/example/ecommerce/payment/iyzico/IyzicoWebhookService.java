package com.example.ecommerce.payment.iyzico;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class IyzicoWebhookService {
    private static final Logger logger = LoggerFactory.getLogger(IyzicoWebhookService.class);

    private final IyzicoProperties properties;
    private final IyzicoPaymentService paymentService;
    private final ObjectMapper objectMapper;

    public IyzicoWebhookService(IyzicoProperties properties,
                                IyzicoPaymentService paymentService,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public void validateAndLogEvent(String payload, String signature) {
        if (properties.isVerifyWebhookSignature()) {
            validateSignature(payload, signature);
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("unknown");
            String paymentId = root.path("paymentId").asText("unknown");
            String conversationId = root.path("paymentConversationId").asText(
                    root.path("conversationId").asText(
                            root.path("paymentConversationIdExternal").asText("")
                    )
            );
            String paymentStatus = root.path("paymentStatus").asText(
                    root.path("status").asText("")
            );
            String errorMessage = root.path("errorMessage").asText("");
            paymentService.processWebhookNotification(eventType, conversationId, paymentId, paymentStatus, errorMessage);
            logger.info("iyzico_webhook_received eventType={} paymentId={} conversationId={}", eventType, paymentId, conversationId);
        } catch (Exception ex) {
            logger.warn("iyzico_webhook_received non_json_payload=true");
        }
    }

    private void validateSignature(String payload, String signature) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("Iyzico webhook signature validation is enabled but webhook secret is missing.");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing Iyzico webhook signature.");
        }

        String expected = hmacSha256Hex(payload, properties.getWebhookSecret());
        String provided = signature.trim().toLowerCase();
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid Iyzico webhook signature.");
        }
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toLowerCase();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }
}
