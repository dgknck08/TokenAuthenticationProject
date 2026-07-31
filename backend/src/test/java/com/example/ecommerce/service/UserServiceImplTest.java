package com.example.ecommerce.service;

import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.exception.UserNotFoundException;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, passwordEncoder);
    }

    private RegisterRequest request() {
        return new RegisterRequest("  Alice ", " Alice@Test.LOCAL ", "secret", " Al ", " Ice ");
    }

    @Test
    void createUser_shouldNormalizeAndEncode() {
        when(passwordEncoder.encode("secret")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createUser(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Alice", saved.getUsername());
        assertEquals("alice@test.local", saved.getEmail());
        assertEquals("Al", saved.getFirstName());
        assertEquals("Ice", saved.getLastName());
        assertEquals("ENC", saved.getPassword());
        assertFalse(saved.isEmailVerified());
        assertTrue(saved.isEnabled());
    }

    @Test
    void getUserById_shouldReturnWhenFound() {
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertSame(user, service.getUserById(1L));
    }

    @Test
    void getUserById_shouldThrowWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.getUserById(99L));
    }

    @Test
    void findByUsername_shouldNormalize() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(User.builder().build()));
        assertTrue(service.findByUsername("  alice ").isPresent());
    }

    @Test
    void findById_shouldDelegate() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertTrue(service.findById(1L).isEmpty());
    }

    @Test
    void save_shouldDelegate() {
        User user = User.builder().id(1L).build();
        when(userRepository.save(user)).thenReturn(user);
        assertSame(user, service.save(user));
    }

    @Test
    void findByEmail_shouldNormalize() {
        when(userRepository.findByEmail("alice@test.local"))
                .thenReturn(Optional.of(User.builder().build()));
        assertTrue(service.findByEmail(" Alice@Test.LOCAL ").isPresent());
    }

    @Test
    void existsByUsername_shouldNormalizeAndDelegate() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertTrue(service.existsByUsername(" alice "));
    }

    @Test
    void existsByEmail_shouldNormalizeAndDelegate() {
        when(userRepository.existsByEmail("alice@test.local")).thenReturn(false);
        assertFalse(service.existsByEmail(" Alice@Test.LOCAL "));
    }

    @Test
    void normalize_shouldHandleNulls() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());
        assertTrue(service.findByUsername(null).isEmpty());
    }
}
