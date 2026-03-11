package com.example.ecommerce.service;

import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.exception.EmailNotVerifiedException;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.EmailVerificationService;
import com.example.ecommerce.auth.service.PasswordResetService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceLoginTest {

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
        user.setEmailVerified(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(accessToken);
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(refreshTokenStr);

        LoginResponse response = authService.login(request);

        assertEquals(username, response.username());
        assertEquals(email, response.email());
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshTokenStr, response.refreshToken());
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenAuthenticationFails() {
        LoginRequest request = new LoginRequest("testuser", "wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenUserDoesNotExist() {
        String username = "notfound";
        LoginRequest request = new LoginRequest(username, "pass");
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
        LoginRequest request = new LoginRequest(username, "testpass");
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("email@example.com");
        user.setEmailVerified(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenThrow(new RuntimeException("Refresh token creation failed"));

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsException_whenUsernameIsBlank() {
        LoginRequest request = new LoginRequest("", "pass");

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
        LoginRequest request = new LoginRequest("user", "pass");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenThrow(new RuntimeException("Token generation error"));

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldCallAllDependencies_inCorrectOrder() {
        String username = "testuser";
        LoginRequest request = new LoginRequest(username, "testpass");
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setEmailVerified(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(1L)).thenReturn("refresh-token");

        authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(authentication);
        verify(userService).findByUsername(username);
        verify(refreshTokenService).createRefreshToken(1L);
    }

    @Test
    void login_shouldThrowEmailNotVerifiedException_whenEmailIsNotVerified() {
        String username = "testuser";
        LoginRequest request = new LoginRequest(username, "testpass");
        Authentication authentication = mock(Authentication.class);

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("email@example.com");
        user.setEmailVerified(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(request));
        verify(refreshTokenService, never()).createRefreshToken(anyLong());
    }
}
