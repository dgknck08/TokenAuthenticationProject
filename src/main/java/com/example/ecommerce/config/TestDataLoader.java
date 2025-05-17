package com.example.ecommerce.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;


import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;

@Component
public class TestDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public TestDataLoader(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("testuser").isEmpty()) {
          
       
        }
    }
}

