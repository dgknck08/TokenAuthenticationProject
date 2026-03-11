package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.model.User;

public interface EmailVerificationService {

    void createAndSendVerification(User user);

    void verifyToken(String rawToken);

    void resendVerification(String email);
}
