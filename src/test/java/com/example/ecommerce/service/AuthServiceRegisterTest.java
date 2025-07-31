package com.example.ecommerce.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.model.RefreshToken;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.service.impl.AuthServiceImpl;

public class AuthServiceRegisterTest {

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
    public void register_ShouldReturnRegisterResponse_whenCredentialsAreValid() {

        String username = "testuser1";
        String email = "test@example.com";
        String password = "testpassword";
        String firstName = "test";
        String lastName = "user";

        RegisterRequest registerRequest = new RegisterRequest(username, email, password, firstName, lastName);

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(username);
        mockUser.setEmail(email);

        String fakeAccessToken = "access-token";
        String fakeRefreshToken = "refresh-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(fakeRefreshToken);


        when(userService.findByUsername(username)).thenReturn(Optional.empty());
        when(userService.findByEmail(email)).thenReturn(Optional.empty());
        when(userService.createUser(registerRequest)).thenReturn(mockUser);
        when(jwtTokenProvider.generateTokenWithUsername(username)).thenReturn(fakeAccessToken);
        when(refreshTokenService.createRefreshToken(mockUser.getId())).thenReturn(refreshToken);


        RegisterResponse response = authService.register(registerRequest);


        assertEquals(username, response.username());
        assertEquals(email, response.email());
        assertEquals(fakeAccessToken, response.accessToken());
        assertEquals(fakeRefreshToken, response.refreshToken());
    }

    @Test
    void register_ShouldThrowException_whenUsernameAlreadyExists() {

        RegisterRequest request = new RegisterRequest("existinguser", "new@example.com", "pass", "Test", "User");

        when(userService.findByUsername("existinguser")).thenReturn(Optional.of(new User()));


        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class, 
            () -> authService.register(request)
        );

        assertEquals("Username is already taken", exception.getMessage());
        verify(userService).findByUsername("existinguser");
        verify(userService, never()).createUser(any());
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {

        RegisterRequest request = new RegisterRequest("newuser", "used@example.com", "password", "ahmet", "hamdi");
        
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("used@example.com")).thenReturn(Optional.of(new User()));

        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> authService.register(request)
        );

        assertEquals("Email is already registered", exception.getMessage());
        verify(userService).findByUsername("newuser");
        verify(userService).findByEmail("used@example.com");
        verify(userService, never()).createUser(any());
    }

    @Test
    void register_ShouldThrowException_whenUserCreationFails() {

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(userService).createUser(request);
    }

    @Test
    void register_ShouldThrowException_whenTokenGenerationFails() {

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("new@example.com");

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenReturn(mockUser);
        when(jwtTokenProvider.generateTokenWithUsername("newuser")).thenThrow(new RuntimeException("Token generation failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.register(request));
    }

    @Test
    void register_ShouldThrowException_whenRefreshTokenCreationFails() {

    	RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("new@example.com");

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenReturn(mockUser);
        when(jwtTokenProvider.generateTokenWithUsername("newuser")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenThrow(new RuntimeException("Refresh token creation failed"));


        assertThrows(RuntimeException.class, () -> authService.register(request));
    }

    @Test
    void register_ShouldCallAllDependencies_inCorrectOrder() {

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("new@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenReturn(mockUser);
        when(jwtTokenProvider.generateTokenWithUsername("newuser")).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshToken);


        authService.register(request);


        verify(userService).findByUsername("newuser");
        verify(userService).findByEmail("new@example.com");
        verify(userService).createUser(request);
        verify(jwtTokenProvider).generateTokenWithUsername("newuser");
        verify(refreshTokenService).createRefreshToken(1L);
    }
}