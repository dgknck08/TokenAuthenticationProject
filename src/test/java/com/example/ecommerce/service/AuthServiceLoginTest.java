package com.example.ecommerce.service;



import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceLoginTest {

    private UserService userService;
    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenService refreshTokenService;
    private AuthenticationManager authenticationManager;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authenticationManager = mock(AuthenticationManager.class);

        authService = new AuthService(userService, jwtTokenProvider, refreshTokenService, authenticationManager);
    }

    @Test
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() {
        String username = "testuser";
        String password = "testpass";
        String accessToken = "access-token";
        String refreshTokenStr = "refresh-token";
        String email = "test@example.com";

        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L); // createRefreshToken için gerekli
        user.setUsername(username);
        user.setEmail(email);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(accessToken);
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertEquals(username, response.getUsername());
        assertEquals(email, response.getEmail());
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshTokenStr, response.getRefreshToken());
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenAuthenticationFails() {
        // Arrange
        String username = "testuser";
        String password = "wrongpass";
        LoginRequest request = new LoginRequest(username, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenUserDoesNotExist() {
        // Arrange
        String username = "notfound";
        String password = "pass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtTokenProvider.generateToken(authentication)).thenReturn("dummy-token");
        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenRefreshTokenIsNull() {
        // Arrange
        String username = "testuser";
        String password = "testpass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("email@example.com");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(null); // NULL dönüyor!

        // Act & Assert
        assertThrows(NullPointerException.class, () -> authService.login(request));
    }
    @Test
    void login_shouldThrowException_whenUsernameIsEmpty() {
        LoginRequest request = new LoginRequest("", "pass");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenPasswordIsEmpty() {
        LoginRequest request = new LoginRequest("user", "");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenAccessTokenGenerationFails() {
        String username = "user";
        String password = "pass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("email@example.com");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenThrow(new RuntimeException("Token error"));
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    
}

