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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

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
    @WithMockUser(username = "user", authorities = {"ORDER_READ"})
    void getOrders_withOrderReadAuthority_returnsOk() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .username("user")
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(orderService.getMyOrders("user")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"ORDER_WRITE"})
    void payOrder_withOrderWriteAuthority_returnsOk() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"ORDER_READ"})
    void payOrder_withoutWriteAuthority_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isForbidden());
    }
}
