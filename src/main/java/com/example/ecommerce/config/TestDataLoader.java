package com.example.ecommerce.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;

@Component
@Profile("dev")
public class TestDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public TestDataLoader(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            refreshTokenRepository.count();
        }
    }
}

