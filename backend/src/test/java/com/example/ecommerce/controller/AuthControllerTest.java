package com.example.ecommerce.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.ecommerce.auth.controller.AuthController;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.dto.RefreshTokenResponse;
import com.example.ecommerce.auth.dto.RegisterResponse;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.AccountLockoutService;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.JwtBlacklistService;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private JwtValidationService jwtValidationService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_whenValidCredentials_returnsOkAndSetsRefreshTokenCookie() throws Exception {
        LoginResponse loginResponse = new LoginResponse("access-token", "refresh-token-value", "testuser", "testuser@example.com");
        when(accountLockoutService.isAccountLocked(any())).thenReturn(false);
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        String loginJson = """
            {
                "username": "testuser",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().value("refreshToken", "refresh-token-value"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void login_whenAccountLocked_returnsLocked() throws Exception {
        when(accountLockoutService.isAccountLocked(any())).thenReturn(true);
        when(accountLockoutService.getAccountLockInfo(any())).thenReturn(Map.of("lockedUntil", "2030-01-01T00:00:00Z"));

        String loginJson = """
            {
                "username": "lockeduser",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void login_whenInvalidCredentials_returnsUnauthorized() throws Exception {
        when(accountLockoutService.isAccountLocked(any())).thenReturn(false);
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException("Invalid credentials"));

        String loginJson = """
            {
                "username": "wronguser",
                "password": "WrongPass123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void register_whenValidRequest_returnsCreatedAndSetsCookie() throws Exception {
        RegisterResponse registerResponse = new RegisterResponse("access-token", "refresh-token-value", "newuser", "newuser@example.com");
        when(authService.register(any())).thenReturn(registerResponse);

        String requestJson = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "Password123!",
                "firstName": "New",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(cookie().value("refreshToken", "refresh-token-value"));
    }

    @Test
    void refreshToken_whenCookiePresent_returnsOkAndNewAccessToken() throws Exception {
        RefreshTokenResponse response = new RefreshTokenResponse("new-access", "new-refresh", "testuser", "test@example.com");
        when(authService.refreshToken("cookie-refresh-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh-token").cookie(new Cookie("refreshToken", "cookie-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void refreshToken_whenServiceReturnsError_returnsBadRequest() throws Exception {
        RefreshTokenResponse response = new RefreshTokenResponse("Invalid or expired refresh token");
        when(authService.refreshToken("cookie-refresh-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh-token").cookie(new Cookie("refreshToken", "cookie-refresh-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logout_whenAccessAndRefreshTokensPresent_blacklistsAndDeletesRefreshToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        request.setCookies(new Cookie("refreshToken", "refresh-token"));
        when(jwtValidationService.getUserIdFromToken("access-token")).thenReturn(7L);

        ResponseEntity<Void> response = authController.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(jwtBlacklistService).blacklistToken("access-token");
        verify(refreshTokenService).deleteByUserId(7L);
        verify(refreshTokenService).deleteByToken("refresh-token");
    }

    @Test
    void logout_whenTokenCannotResolveUserId_stillReturnsNoContent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        when(jwtValidationService.getUserIdFromToken("access-token")).thenThrow(new RuntimeException("bad token"));

        ResponseEntity<Void> response = authController.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(jwtBlacklistService).blacklistToken("access-token");
        verify(refreshTokenService, never()).deleteByUserId(any());
    }

    @Test
    void logoutAll_whenAuthenticated_blacklistsUserTokensAndDeletesRefreshTokens() {
        User user = User.builder().id(11L).username("alice").build();
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));

        ResponseEntity<Void> response = authController.logoutAll();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(jwtBlacklistService).blacklistUserTokens("alice");
        verify(refreshTokenService).deleteByUserId(11L);
    }

    @Test
    void verifyToken_whenNoToken_returnsBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Map<String, Object>> response = authController.verifyToken(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("valid"));
    }

    @Test
    void verifyToken_whenValidToken_returnsMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        when(jwtValidationService.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("access-token")).thenReturn("alice");
        when(jwtTokenProvider.getRolesFromToken("access-token")).thenReturn(List.of("ROLE_USER"));
        when(jwtBlacklistService.getTokenMetadata("access-token")).thenReturn(Map.of("ipAddress", "127.0.0.1"));

        ResponseEntity<Map<String, Object>> response = authController.verifyToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("valid"));
        assertEquals("alice", response.getBody().get("username"));
        assertNotNull(response.getBody().get("metadata"));
    }

    @Test
    void accountStatus_returnsLockAndAttemptInfo() {
        when(accountLockoutService.isAccountLocked("alice")).thenReturn(true);
        when(accountLockoutService.getFailedAttemptCount("alice")).thenReturn(4);
        when(accountLockoutService.getAccountLockInfo("alice")).thenReturn(Map.of("lockedUntil", "2030-01-01T00:00:00Z"));

        ResponseEntity<Map<String, Object>> response = authController.getAccountStatus("alice");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("locked"));
        assertEquals(4, response.getBody().get("failedAttempts"));
    }

    @Test
    void unlockAccount_callsServiceAndReturnsMessage() {
        ResponseEntity<Map<String, String>> response = authController.unlockAccount("alice");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Account unlocked successfully", response.getBody().get("message"));
        verify(accountLockoutService).unlockAccount(eq("alice"));
    }
}
