package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.*;
import com.example.ecommerce.auth.handler.AuthResponseHandler;
import com.example.ecommerce.auth.service.AuthService;
import com.example.ecommerce.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return AuthResponseHandler.handleLogin(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return AuthResponseHandler.handleRegister(registerResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtil.getRefreshTokenFromCookie(request);
        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(refreshToken);
        return AuthResponseHandler.handleRefreshTokenResponse(refreshTokenResponse);
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return AuthResponseHandler.handleLogout();
    }

}
