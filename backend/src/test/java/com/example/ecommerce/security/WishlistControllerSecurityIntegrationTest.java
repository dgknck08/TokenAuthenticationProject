package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.wishlist.controller.WishlistController;
import com.example.ecommerce.wishlist.dto.WishlistItemResponse;
import com.example.ecommerce.wishlist.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WishlistController.class)
@Import(SecurityConfig.class)
class WishlistControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtValidationService jwtValidationService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getWishlist_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWishlist_withWishlistReadAuthority_returnsOk() throws Exception {
        stubToken("token-read", "user", "WISHLIST_READ");
        when(wishlistService.getMyWishlist("user")).thenReturn(List.of(
                WishlistItemResponse.builder()
                        .id(1L)
                        .productId(5L)
                        .productName("Guitar")
                        .productPrice(new BigDecimal("999.90"))
                        .build()
        ));

        mockMvc.perform(get("/api/wishlist")
                        .header("Authorization", "Bearer token-read"))
                .andExpect(status().isOk());
    }

    @Test
    void addWishlistItem_withWishlistWriteAuthority_returnsOk() throws Exception {
        stubToken("token-write", "user", "WISHLIST_WRITE");
        when(wishlistService.addItem("user", 7L)).thenReturn(
                WishlistItemResponse.builder()
                        .id(2L)
                        .productId(7L)
                        .productName("Piano")
                        .productPrice(new BigDecimal("1499.00"))
                        .build()
        );

        mockMvc.perform(put("/api/wishlist/7")
                        .header("Authorization", "Bearer token-write"))
                .andExpect(status().isOk());
    }

    @Test
    void addWishlistItem_withoutWriteAuthority_returnsForbidden() throws Exception {
        stubToken("token-read-only", "user", "WISHLIST_READ");
        mockMvc.perform(put("/api/wishlist/7")
                        .header("Authorization", "Bearer token-read-only"))
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
