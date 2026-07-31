package com.example.ecommerce.service;

import com.example.ecommerce.payment.iyzico.IyzicoPaymentService;
import com.example.ecommerce.payment.iyzico.IyzicoProperties;
import com.example.ecommerce.payment.iyzico.IyzicoWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IyzicoWebhookServiceTest {

    @Mock
    private IyzicoProperties properties;
    @Mock
    private IyzicoPaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IyzicoWebhookService service;

    @BeforeEach
    void setUp() {
        service = new IyzicoWebhookService(properties, paymentService, objectMapper);
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void validateAndLogEvent_shouldForwardNotificationWhenSignatureDisabled() {
        when(properties.isVerifyWebhookSignature()).thenReturn(false);
        String payload = "{\"eventType\":\"PAYMENT\",\"paymentId\":\"p1\","
                + "\"paymentConversationId\":\"c1\",\"paymentStatus\":\"SUCCESS\"}";

        service.validateAndLogEvent(payload, null);

        verify(paymentService).processWebhookNotification("PAYMENT", "c1", "p1", "SUCCESS", "");
    }

    @Test
    void validateAndLogEvent_shouldUseFallbackConversationAndStatusFields() {
        when(properties.isVerifyWebhookSignature()).thenReturn(false);
        String payload = "{\"eventType\":\"PAYMENT\",\"paymentId\":\"p1\","
                + "\"conversationId\":\"fallbackConv\",\"status\":\"FAILURE\",\"errorMessage\":\"declined\"}";

        service.validateAndLogEvent(payload, null);

        verify(paymentService).processWebhookNotification("PAYMENT", "fallbackConv", "p1", "FAILURE", "declined");
    }

    @Test
    void validateAndLogEvent_shouldAcceptValidSignature() throws Exception {
        String payload = "{\"eventType\":\"PAYMENT\",\"paymentId\":\"p1\"}";
        when(properties.isVerifyWebhookSignature()).thenReturn(true);
        when(properties.getWebhookSecret()).thenReturn("topsecret");

        service.validateAndLogEvent(payload, hmac(payload, "topsecret"));

        verify(paymentService).processWebhookNotification(eq("PAYMENT"), eq(""), eq("p1"), eq(""), eq(""));
    }

    @Test
    void validateAndLogEvent_shouldRejectMissingSecret() {
        when(properties.isVerifyWebhookSignature()).thenReturn(true);
        when(properties.getWebhookSecret()).thenReturn("  ");

        assertThrows(IllegalStateException.class,
                () -> service.validateAndLogEvent("{}", "sig"));
    }

    @Test
    void validateAndLogEvent_shouldRejectMissingSignature() {
        when(properties.isVerifyWebhookSignature()).thenReturn(true);
        when(properties.getWebhookSecret()).thenReturn("topsecret");

        assertThrows(IllegalArgumentException.class,
                () -> service.validateAndLogEvent("{}", null));
    }

    @Test
    void validateAndLogEvent_shouldRejectInvalidSignature() {
        when(properties.isVerifyWebhookSignature()).thenReturn(true);
        when(properties.getWebhookSecret()).thenReturn("topsecret");

        assertThrows(IllegalArgumentException.class,
                () -> service.validateAndLogEvent("{}", "deadbeef"));
    }

    @Test
    void validateAndLogEvent_shouldSwallowNonJsonPayload() {
        when(properties.isVerifyWebhookSignature()).thenReturn(false);

        service.validateAndLogEvent("this-is-not-json", null);

        verify(paymentService, never()).processWebhookNotification(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
