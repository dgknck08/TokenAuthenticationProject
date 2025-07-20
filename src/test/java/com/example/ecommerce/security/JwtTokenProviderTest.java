package com.example.ecommerce.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.security.JwtTokenProvider;

import java.util.List;

class JwtTokenProviderTest {

    @Mock
    private UserDetailsService userDetailsService;

    private JwtTokenProvider jwtTokenProvider;

    private final String secret = "01234567890123456789012345678901234567890123456789012345678901234567890"; 
    private final int expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtTokenProvider = new JwtTokenProvider(userDetailsService, secret, expirationMs);
    }

    @Test
    void testGenerateTokenWithUsername_simple() {
        String username = "user1";
        String token = jwtTokenProvider.generateTokenWithUsername(username);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testGenerateTokenWithUsername_withRoles() {
        String username = "user1";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        String token = jwtTokenProvider.generateTokenWithUsername(username, roles);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }


    @Test
    void testGetAuthentication() {
        UserDetails userDetails = new User("authUser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("authUser")).thenReturn(userDetails);

        String token = jwtTokenProvider.generateTokenWithUsername("authUser");
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        assertNotNull(authentication);
        assertEquals("authUser", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
    

    @Test
    public void testGenerateToken() {
        // Hazır Authentication objesi oluşturuyoruz
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Token üret
        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // İstersen token içinden username ve claim'leri parse edip test edebilirsin (JwtTokenProvider metodları ile)
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testUser", username);
    }
    
    @Test
    void testValidateToken_ValidToken() {
        String token = jwtTokenProvider.generateTokenWithUsername("validUser");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    
    @Test
    public void testValidateToken_expiredToken_throwsJwtValidationException() throws InterruptedException {
        // Çok kısa süreli token 
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                Mockito.mock(UserDetailsService.class),
                "1234567890123456789012345678901234567890123456789012345678901234",
                1000 // 1 saniye
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = shortLivedProvider.generateToken(authentication);

        // Token'ın süresi doluyor.
        Thread.sleep(2000);

        // ExpiredJwtException 
        JwtValidationException exception = assertThrows(JwtValidationException.class, () -> {
            shortLivedProvider.validateToken(token);
        });

        assertTrue(exception.getMessage().contains("expired"));
    }

    
    @Test
    void testValidateToken_InvalidToken() {
        String invalidToken = "invalid.token.value";
        assertThrows(JwtValidationException.class, () -> jwtTokenProvider.validateToken(invalidToken));
    }
}
