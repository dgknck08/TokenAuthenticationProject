package com.example.ecommerce.security;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.review.controller.ProductReviewController;
import com.example.ecommerce.review.dto.ProductReviewResponse;
import com.example.ecommerce.review.service.ProductReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductReviewController.class)
@Import(SecurityConfig.class)
class ProductReviewControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductReviewService productReviewService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtValidationService jwtValidationService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getReviews_shouldBePublic() throws Exception {
        ProductReviewResponse response = ProductReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(15L)
                .username("alice")
                .rating(5)
                .comment("Kaliteli urun tavsiye ederim")
                .build();
        when(productReviewService.getProductReviews(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/products/10/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void createReview_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/products/10/reviews")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"Kaliteli urun tavsiye ederim"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReview_shouldRequireOrderWriteAuthority() throws Exception {
        stubToken("token-product-read", "user", "PRODUCT_READ");

        mockMvc.perform(post("/api/products/10/reviews")
                        .header("Authorization", "Bearer token-product-read")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"Kaliteli urun tavsiye ederim"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReview_withOrderWriteAuthority_shouldReturnCreated() throws Exception {
        stubToken("token-order-write", "user", "ORDER_WRITE");
        ProductReviewResponse response = ProductReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(15L)
                .username("user")
                .rating(5)
                .comment("Kaliteli urun tavsiye ederim")
                .build();
        when(productReviewService.createReview(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/products/10/reviews")
                        .header("Authorization", "Bearer token-order-write")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rating":5,"comment":"Kaliteli urun tavsiye ederim"}
                                """))
                .andExpect(status().isCreated());
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
