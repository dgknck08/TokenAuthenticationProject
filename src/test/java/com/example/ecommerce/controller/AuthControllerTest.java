package com.example.ecommerce.controller;


import com.example.ecommerce.auth.controller.AuthController;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.service.AccountLockoutService;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccountLockoutService accountLockoutService;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    @MockBean
    private JwtValidationService jwtValidationService;  

    @Test
    public void login_whenValidCredentials_returnsOk() throws Exception {
        LoginResponse loginResponse = new LoginResponse(
            "access-token", 
            "refresh-token", 
            "testuser", 
            "testuser@example.com"
        );
        Mockito.when(accountLockoutService.isAccountLocked(any())).thenReturn(false);
        Mockito.when(authService.login(any())).thenReturn(loginResponse);

        String loginJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.username").value("testuser"));
    }
    @Test
    public void login_whenAccountLocked_returnsLocked() throws Exception {
        Mockito.when(accountLockoutService.isAccountLocked(any())).thenReturn(true);

        String loginJson = """
            {
                "username": "lockeduser",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void login_whenInvalidCredentials_returnsUnauthorized() throws Exception {
        Mockito.when(accountLockoutService.isAccountLocked(any())).thenReturn(false);
        Mockito.when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid credentials"));

        String loginJson = """
            {
                "username": "wronguser",
                "password": "wrongpass"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
