package com.example.ecommerce.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.cart.exception.CartExceptionHandler;
import com.example.ecommerce.cart.exception.CartItemNotFoundException;
import com.example.ecommerce.cart.exception.CartNotFoundException;
import com.example.ecommerce.cart.exception.InsufficientStockException;

class CartExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CartExceptionHandler())
                .build();
    }

    @Test
    void cartNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/test/cart-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
    }

    @Test
    void cartItemNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/test/cart-item-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void insufficientStock_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/stock"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/cart-not-found")
        ResponseEntity<Void> cartNotFound() {
            throw new CartNotFoundException("cart not found");
        }

        @GetMapping("/test/cart-item-not-found")
        ResponseEntity<Void> cartItemNotFound() {
            throw new CartItemNotFoundException("cart item not found");
        }

        @GetMapping("/test/stock")
        ResponseEntity<Void> insufficientStock() {
            throw new InsufficientStockException("not enough stock");
        }
    }
}
