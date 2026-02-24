package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.order.controller.OrderController;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentMethod;
import com.example.ecommerce.order.service.OrderService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtValidationService jwtValidationService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getOrders_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrders_withOrderReadAuthority_returnsOk() throws Exception {
        stubToken("token-order-read", "user", "ORDER_READ");
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .username("user")
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(orderService.getMyOrders(org.mockito.ArgumentMatchers.eq("user"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer token-order-read"))
                .andExpect(status().isOk());
    }

    @Test
    void payOrder_withOrderWriteAuthority_returnsOk() throws Exception {
        stubToken("token-order-write", "user", "ORDER_WRITE");
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .username("user")
                .status(OrderStatus.PAID)
                .paymentMethod(PaymentMethod.CARD)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(orderService.payMyOrder(org.mockito.ArgumentMatchers.eq("user"), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/orders/1/pay")
                        .header("Authorization", "Bearer token-order-write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void payOrder_withoutWriteAuthority_returnsForbidden() throws Exception {
        stubToken("token-order-read-only", "user", "ORDER_READ");
        mockMvc.perform(post("/api/orders/1/pay")
                        .header("Authorization", "Bearer token-order-read-only")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isForbidden());
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
