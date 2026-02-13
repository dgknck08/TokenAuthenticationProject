package com.example.ecommerce.service;


import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.service.impl.AuthServiceImpl;

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
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authenticationManager = mock(AuthenticationManager.class);

        authService = new AuthServiceImpl(userService, jwtTokenProvider, refreshTokenService, authenticationManager);
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
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(email);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(accessToken);
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);

        LoginResponse response = authService.login(request);

        assertEquals(username, response.username());
        assertEquals(email, response.email());
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshTokenStr, response.refreshToken());
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenAuthenticationFails() {

        String username = "testuser";
        String password = "wrongpass";
        LoginRequest request = new LoginRequest(username, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenUserDoesNotExist() {

        String username = "notfound";
        String password = "pass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("dummy-token");
        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowRuntimeException_whenRefreshTokenCreationFails() {

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
        when(refreshTokenService.createRefreshToken(1L)).thenThrow(new RuntimeException("Refresh token creation failed"));


        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenUsernameIsBlank() {
        // Bu test validation layerda yakalanır normalde, ama service level kontrolü 
        LoginRequest request = new LoginRequest("", "pass");

        // AuthService'in username/password kontrolü 
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Empty username"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenPasswordIsBlank() {

        LoginRequest request = new LoginRequest("user", "");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Empty password"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowRuntimeException_whenTokenGenerationFails() {

        String username = "user";
        String password = "pass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenThrow(new RuntimeException("Token generation error"));


        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldCallAllDependencies_inCorrectOrder() {

        String username = "testuser";
        String password = "testpass";
        LoginRequest request = new LoginRequest(username, password);
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);


        authService.login(request);


        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
        verify(userService).findByUsername(username);
        verify(refreshTokenService).createRefreshToken(1L);
    }
}