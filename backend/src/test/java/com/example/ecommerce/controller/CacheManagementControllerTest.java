package com.example.ecommerce.controller;

import com.example.ecommerce.auth.controller.CacheManagementController;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.service.JwtValidationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CacheManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
public class CacheManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtValidationService jwtValidationService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @org.springframework.boot.test.mock.mockito.MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @WithMockUser(username = "admin", authorities = {"AUDIT_READ", "AUDIT_WRITE"})
    public void stats_whenAdmin_returnsOk() throws Exception {
        Map<String, Object> stats = Map.of(
            "jwtClaimsCache", Map.of("size", 1),
            "jwtValidationCache", Map.of("size", 2),
            "userDetailsCache", Map.of("size", 3)
        );
        Mockito.when(jwtTokenProvider.getCacheStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/cache/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jwtClaimsCache.size").value(1))
            .andExpect(jsonPath("$.jwtValidationCache.size").value(2))
            .andExpect(jsonPath("$.userDetailsCache.size").value(3));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"AUDIT_WRITE"})
    public void clearUserCache_whenAdmin_invokesService() throws Exception {
        mockMvc.perform(post("/api/admin/cache/clear/testuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Cache cleared for user: testuser"));

        verify(jwtValidationService).invalidateUserTokens("testuser");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"AUDIT_WRITE"})
    public void clearAllCaches_whenAdmin_invokesService() throws Exception {
        mockMvc.perform(post("/api/admin/cache/clear/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("All caches cleared"));

        verify(jwtTokenProvider).invalidateAllCaches();
    }

    @Test
    @WithMockUser(username = "user", authorities = {"PRODUCT_READ"})
    public void endpoints_whenNonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/cache/stats"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/cache/clear/testuser"))
            .andExpect(status().isForbidden());
    }
}
