package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LoginResponse;
import com.example.ecommerce.auth.dto.RefreshTokenResponse;
import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.dto.RegisterResponse;

public interface AuthService {
	 	RegisterResponse register(RegisterRequest request);

	    LoginResponse login(LoginRequest request);

	    RefreshTokenResponse refreshToken(String refreshToken);
}
