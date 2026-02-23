package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.order.controller.AdminOrderController;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminOrderController.class)
@Import(SecurityConfig.class)
class AdminOrderControllerSecurityIntegrationTest {

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
    @WithMockUser(username = "mod", roles = {"MODERATOR"})
    void adminOrders_withNonAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminOrders_withAdminRole_returnsOk() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(10L)
                .username("user")
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        when(orderService.getAllOrdersForAdmin()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminRefund_withAdminRole_returnsOk() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(11L)
                .username("user")
                .status(OrderStatus.REFUNDED)
                .totalAmount(new BigDecimal("120.00"))
                .build();
        when(orderService.refundOrderForAdmin(11L, "admin")).thenReturn(response);

        mockMvc.perform(post("/api/admin/orders/11/refund"))
                .andExpect(status().isOk());
    }
}
