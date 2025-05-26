package com.example.ecommerce.auth.service;


import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.model.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    User save(User user);
    User getUserById(Long id);
    User createUser(RegisterRequest request);
    
    Optional<User> findByEmail(String email);

}
