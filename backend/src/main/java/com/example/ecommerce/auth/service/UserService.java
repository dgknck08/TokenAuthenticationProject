package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.model.User;

import java.util.Optional;

public interface UserService {
    User createUser(RegisterRequest request);
    User getUserById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
