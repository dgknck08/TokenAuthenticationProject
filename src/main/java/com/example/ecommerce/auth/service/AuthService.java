package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.auth.exception.InvalidCredentialsException;
import com.example.ecommerce.auth.exception.UserAlreadyExistsException;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.security.JwtTokenProvider;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

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

    public RegisterResponse register(RegisterRequest request) {
        if (userService.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Kullanıcı zaten kayıtlı");
        }

        User user = userService.createUser(request);

        String accessToken = jwtTokenProvider.generateTokenWithUsername(user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        return new RegisterResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String accessToken = jwtTokenProvider.generateToken(authentication);

            User user = userService.findByUsername(request.getUsername())
                                   .orElseThrow(() -> new UserNotFoundException("User not found"));

            String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

            return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        String username = refreshTokenService.validateRefreshToken(refreshToken);
        User user = userService.findByUsername(username)
                               .orElseThrow(() -> new UserNotFoundException("User not found"));
        String accessToken = jwtTokenProvider.generateTokenWithUsername(username);
        return new RefreshTokenResponse(accessToken, refreshToken, user.getUsername(), user.getEmail());
    }
}
