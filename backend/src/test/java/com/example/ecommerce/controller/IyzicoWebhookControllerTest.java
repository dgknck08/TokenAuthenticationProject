package com.example.ecommerce.controller;

import com.example.ecommerce.payment.iyzico.IyzicoWebhookController;
import com.example.ecommerce.payment.iyzico.IyzicoWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IyzicoWebhookControllerTest {

    @Mock
    private IyzicoWebhookService webhookService;

    @InjectMocks
    private IyzicoWebhookController webhookController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    void webhook_shouldReturnOk() throws Exception {
        String payload = "{\"eventType\":\"PAYMENT_SUCCEEDED\"}";

        mockMvc.perform(post("/api/payments/iyzico/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-iyzi-signature-v3", "sig")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(webhookService).validateAndLogEvent(payload, "sig");
    }
}
