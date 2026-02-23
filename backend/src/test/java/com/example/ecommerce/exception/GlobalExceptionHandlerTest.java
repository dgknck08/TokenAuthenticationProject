package com.example.ecommerce.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.auth.exception.GlobalExceptionHandler;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.exception.JwtValidationException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidCredentialsException_returnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/test/invalid-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.path").value("/test/invalid-credentials"));
    }

    @Test
    void jwtValidationException_returnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/test/jwt-invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JWT_INVALID"));
    }

    @Test
    void accessDeniedException_returnsForbiddenResponse() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/invalid-credentials")
        ResponseEntity<Void> invalidCredentials() {
            throw new InvalidCredentialsException("bad credentials");
        }

        @GetMapping("/test/jwt-invalid")
        ResponseEntity<Void> jwtInvalid() {
            throw new JwtValidationException("invalid token");
        }

        @GetMapping("/test/forbidden")
        ResponseEntity<Void> forbidden() {
            throw new AccessDeniedException("forbidden");
        }
    }
}
