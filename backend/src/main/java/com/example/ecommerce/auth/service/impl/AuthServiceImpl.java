package com.example.ecommerce.auth.service.impl;

import com.example.ecommerce.auth.dto.*;

import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.auth.service.RefreshTokenService;
import com.example.ecommerce.auth.service.UserService;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService{
    
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    private final UserService userService;  
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserService userService,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    
    /*
     * client->bilgiler ile register olur.
     * acccestoken jwtprovider ile bodyde yollanmak için hazırlanır.
     * refreshtoken oluşturulur dbye kaydedilir httponly ile cliente iletmek için hazırlanır.
     * 
     * */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        String normalizedEmail = normalizeEmail(request.email());
        RegisterRequest normalizedRequest = new RegisterRequest(
            normalizedUsername,
            normalizedEmail,
            request.password(),
            normalizeName(request.firstName()),
            normalizeName(request.lastName())
        );

        logger.info("Registering new user: {}", normalizedUsername);
        
        validateRegisterRequest(normalizedRequest);
        
        User user = userService.createUser(normalizedRequest);
        String accessToken = jwtTokenProvider.generateTokenWithUsername(
            user.getUsername(),
            user.getRoles().stream().map(r -> r.name()).toList()
        );
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        logger.info("User registered successfully: {}", user.getUsername());
        return new RegisterResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
    }

    /**
     * client -> username ve password ile doğrulamasını yapar.
     * Doğrulama başarılı olursa -> (access token) ve (refresh token) üretir.
     * Üretilen tokenlar ve kullanıcı bilgileri ile LoginResponse nesnesi döner.
     *
     * @param request username ve password bilgilerini içeren LoginRequest 
     * @return acces tokenı refresh tokenı username ve eposta bilgilerini içeren LoginResponse 
     * @throws InvalidCredentialsException doğrulama başarısızsa fırlatılır
     */
    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        logger.info("Login attempt for user: {}", normalizedUsername);
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, request.password())
            );
            
            String accessToken = jwtTokenProvider.generateToken(authentication);
            User user = userService.findByUsername(normalizedUsername)
                                   .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
            
            String refreshToken = refreshTokenService.createRefreshToken(user.getId());
            
            logger.info("User logged in successfully: {}", user.getUsername());
            return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
            
        } catch (BadCredentialsException ex) {
            logger.warn("Failed login attempt for user: {}", normalizedUsername);
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
    /**
     * client -> refreshtoken ile yeni accestoken almak için buraya gelir.
     * refreshtoken valid ise yeni accestoken valid değil ise login olmali
     */
    public RefreshTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            logger.warn("Refresh token request with empty token");
            return new RefreshTokenResponse("Refresh token is required");
        }
        
        try {
            String username = refreshTokenService.validateRefreshToken(refreshToken);
            User user = userService.findByUsername(username)
                                   .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            String accessToken = jwtTokenProvider.generateTokenWithUsername(
                username,
                user.getRoles().stream().map(role -> role.name()).toList()
            );
            
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

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
