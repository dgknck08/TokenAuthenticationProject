package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.order.controller.AdminReturnRequestController;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminReturnRequestController.class)
@Import(SecurityConfig.class)
class AdminReturnRequestControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReturnRequestService returnRequestService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtValidationService jwtValidationService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void adminReturnRequests_withNonAdminRole_returnsForbidden() throws Exception {
        stubToken("token-mod", "mod", "ROLE_MODERATOR");

        mockMvc.perform(get("/api/admin/return-requests")
                        .header("Authorization", "Bearer token-mod"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReturnRequests_withAdminRole_returnsOk() throws Exception {
        stubToken("token-admin-list", "admin", "ROLE_ADMIN");
        ReturnRequestResponse response = ReturnRequestResponse.builder()
                .id(4L)
                .orderId(15L)
                .status(ReturnRequestStatus.REQUESTED)
                .reason("Damaged")
                .build();
        when(returnRequestService.getAllReturnRequestsForAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/admin/return-requests")
                        .header("Authorization", "Bearer token-admin-list"))
                .andExpect(status().isOk());
    }

    @Test
    void approveReturnRequest_withAdminRole_returnsOk() throws Exception {
        stubToken("token-admin-approve", "admin", "ROLE_ADMIN");
        ReturnRequestResponse response = ReturnRequestResponse.builder()
                .id(4L)
                .orderId(15L)
                .status(ReturnRequestStatus.APPROVED)
                .reason("Damaged")
                .build();
        when(returnRequestService.approveReturnRequest(4L, "admin")).thenReturn(response);

        mockMvc.perform(post("/api/admin/return-requests/4/approve")
                        .header("Authorization", "Bearer token-admin-approve"))
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
