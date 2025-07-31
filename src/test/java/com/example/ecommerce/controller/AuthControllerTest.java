package com.example.ecommerce.controller;

import com.example.ecommerce.auth.controller.AuthController;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.service.AccountLockoutService;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.auth.security.SecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private JwtValidationService jwtValidationService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
       
                .build();
    }

    @Test
    public void login_whenValidCredentials_returnsOk() throws Exception {
        LoginResponse loginResponse = new LoginResponse(
            "access-token",
            "refresh-token",
            "testuser",
            "testuser@example.com"
        );

        Mockito.when(accountLockoutService.isAccountLocked(any())).thenReturn(false);
        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

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
        Mockito.when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException("Invalid credentials"));

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
