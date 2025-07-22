package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserService userService;  
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserService userService,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        logger.info("Registering new user: {}", request.username());
        
        validateRegisterRequest(request);
        
        User user = userService.createUser(request);
        String accessToken = jwtTokenProvider.generateTokenWithUsername(user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
        
        logger.info("User registered successfully: {}", user.getUsername());
        return new RegisterResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for user: {}", request.username());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            
            String accessToken = jwtTokenProvider.generateToken(authentication);
            User user = userService.findByUsername(request.username())
                                   .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
            
            String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();
            
            logger.info("User logged in successfully: {}", user.getUsername());
            return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
            
        } catch (BadCredentialsException ex) {
            logger.warn("Failed login attempt for user: {}", request.username());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            logger.warn("Refresh token request with empty token");
            return new RefreshTokenResponse("Refresh token is required");
        }
        
        try {
            String username = refreshTokenService.validateRefreshToken(refreshToken);
            User user = userService.findByUsername(username)
                                   .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            String accessToken = jwtTokenProvider.generateTokenWithUsername(username);
            
            logger.info("Token refreshed successfully for user: {}", username);
            return new RefreshTokenResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
            
        } catch (Exception ex) {
            logger.error("Failed to refresh token: {}", ex.getMessage());
            return new RefreshTokenResponse("Invalid or expired refresh token");
        }
    }
    
    private void validateRegisterRequest(RegisterRequest request) {
        if (userService.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userService.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
    }
}
