package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.example.ecommerce.payment.iyzico.IyzicoPaymentController;
import com.example.ecommerce.payment.iyzico.IyzicoPaymentService;
import com.example.ecommerce.payment.iyzico.IyzicoProperties;
import com.example.ecommerce.payment.iyzico.dto.IyzicoPaymentCallbackResponse;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentProviderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IyzicoPaymentController.class)
@Import(SecurityConfig.class)
class IyzicoPaymentControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IyzicoPaymentService paymentService;

    @MockBean
    private IyzicoProperties iyzicoProperties;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtValidationService jwtValidationService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void initPayment_requiresAuthentication() throws Exception {
        when(idempotencyService.findReplayResponse(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/payments/iyzico/orders/1/init")
                        .contentType("application/json")
                        .content("{\"locale\":\"tr\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void callback_isPublic() throws Exception {
        when(iyzicoProperties.getFrontendBaseUrl()).thenReturn("");
        when(paymentService.handleCallback("token-123", null))
                .thenReturn(IyzicoPaymentCallbackResponse.builder()
                        .orderId(1L)
                        .conversationId("conv-1")
                        .paymentReferenceId("pay-1")
                        .paymentStatus(PaymentProviderStatus.SUCCESS)
                        .orderStatus(OrderStatus.PAID)
                        .success(true)
                        .message("ok")
                        .build());

        mockMvc.perform(post("/api/payments/iyzico/callback")
                        .param("token", "token-123")
                        .param("redirect", "false"))
                .andExpect(status().isOk());
    }
}
