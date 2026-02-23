package com.example.ecommerce.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.security.SecurityConfig;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.product.controller.ProductController;
import com.example.ecommerce.product.dto.ProductDto;
import com.example.ecommerce.product.service.ProductService;

@WebMvcTest(controllers = ProductController.class)
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtValidationService jwtValidationService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getProducts_isPublic_returnsOkWithoutToken() throws Exception {
        ProductDto dto = new ProductDto(1L, "Phone", "Desc", new BigDecimal("10.00"), "img", "Cat");
        when(productService.getAllProducts()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void createProduct_isProtected_returnsUnauthorizedWithoutToken() throws Exception {
        String requestJson = """
            {
              "name": "Phone",
              "description": "Desc",
              "price": 10.0,
              "imageUrl": "https://a.com/p.png",
              "category": "Cat",
              "stock": 5
            }
            """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }
}
