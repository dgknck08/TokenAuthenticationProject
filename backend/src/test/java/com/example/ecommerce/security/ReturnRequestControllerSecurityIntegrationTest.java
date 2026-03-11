package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.common.idempotency.IdempotencyService;
import com.example.ecommerce.order.controller.ReturnRequestController;
import com.example.ecommerce.order.dto.ReturnRequestResponse;
import com.example.ecommerce.order.model.ReturnRequestStatus;
import com.example.ecommerce.order.service.ReturnRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReturnRequestController.class)
@Import(SecurityConfig.class)
class ReturnRequestControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReturnRequestService returnRequestService;

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
    void createReturnRequest_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/orders/1/return-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Damaged\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnRequest_withOrderWriteAuthority_returnsCreated() throws Exception {
        stubToken("token-order-write", "user", "ORDER_WRITE");
        ReturnRequestResponse response = ReturnRequestResponse.builder()
                .id(1L)
                .orderId(10L)
                .status(ReturnRequestStatus.REQUESTED)
                .reason("Damaged")
                .build();
        when(idempotencyService.findReplayResponse(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(returnRequestService.createMyReturnRequest("user", 10L, "Damaged")).thenReturn(response);

        mockMvc.perform(post("/api/orders/10/return-requests")
                        .header("Authorization", "Bearer token-order-write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Damaged\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getMyReturnRequests_withOrderReadAuthority_returnsOk() throws Exception {
        stubToken("token-order-read", "user", "ORDER_READ");
        ReturnRequestResponse response = ReturnRequestResponse.builder()
                .id(1L)
                .orderId(10L)
                .status(ReturnRequestStatus.REQUESTED)
                .reason("Damaged")
                .build();
        when(returnRequestService.getMyReturnRequests(org.mockito.ArgumentMatchers.eq("user"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/orders/return-requests")
                        .header("Authorization", "Bearer token-order-read"))
                .andExpect(status().isOk());
    }

    private void stubToken(String token, String username, String authority) {
        when(jwtValidationService.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(token)).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );
        when(jwtTokenProvider.getTokenId(token)).thenReturn("jti-" + token);
    }
}
