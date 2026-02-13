package com.example.ecommerce.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RefreshTokenResponse {
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private String username;
    private String email;
    private String message; // Error durumunda kullanılır

    public RefreshTokenResponse() {}

    public RefreshTokenResponse(String accessToken, String refreshToken, String username, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.email = email;
    }

    public RefreshTokenResponse(String message) {
        this.message = message;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isSuccess() {
        return message == null;
    }
    
    public boolean hasError() {
        return message != null;
    }
}