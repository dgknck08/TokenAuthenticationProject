package com.example.ecommerce.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;
import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.EmailVerificationService;
import com.example.ecommerce.auth.service.PasswordResetService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.auth.service.impl.AuthServiceImpl;

public class AuthServiceRegisterTest {

    private UserService userService;
    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenService refreshTokenService;
    private EmailVerificationService emailVerificationService;
    private PasswordResetService passwordResetService;
    private AuthenticationManager authenticationManager;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailVerificationService = mock(EmailVerificationService.class);
        passwordResetService = mock(PasswordResetService.class);
        authenticationManager = mock(AuthenticationManager.class);

        authService = new AuthServiceImpl(
                userService,
                jwtTokenProvider,
                refreshTokenService,
                emailVerificationService,
                passwordResetService,
                authenticationManager
        );
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
        mockUser.setRoles(Set.of(Role.ROLE_USER));

        when(userService.findByUsername(username)).thenReturn(Optional.empty());
        when(userService.findByEmail(email)).thenReturn(Optional.empty());
        when(userService.createUser(registerRequest)).thenReturn(mockUser);


        RegisterResponse response = authService.register(registerRequest);


        assertEquals(username, response.username());
        assertEquals(email, response.email());
        assertEquals(null, response.accessToken());
        assertEquals(null, response.refreshToken());
        verify(emailVerificationService).createAndSendVerification(mockUser);
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
    void register_ShouldThrowException_whenVerificationCreationFails() {

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("new@example.com");
        mockUser.setRoles(Set.of(Role.ROLE_USER));

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenReturn(mockUser);
        doThrow(new RuntimeException("Verification creation failed"))
            .when(emailVerificationService).createAndSendVerification(mockUser);

        assertThrows(RuntimeException.class, () -> authService.register(request));
    }

    @Test
    void register_ShouldCallAllDependencies_inCorrectOrder() {

        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password", "Test", "User");
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("newuser");
        mockUser.setEmail("new@example.com");
        mockUser.setRoles(Set.of(Role.ROLE_USER));

        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userService.createUser(request)).thenReturn(mockUser);
        authService.register(request);


        verify(userService).findByUsername("newuser");
        verify(userService).findByEmail("new@example.com");
        verify(userService).createUser(request);
        verify(emailVerificationService).createAndSendVerification(mockUser);
    }
}
