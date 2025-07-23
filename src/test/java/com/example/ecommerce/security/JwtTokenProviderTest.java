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
import com.example.ecommerce.auth.service.JwtBlacklistService;

import java.util.List;

class JwtTokenProviderTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtBlacklistService blacklistService;
    
    private JwtTokenProvider jwtTokenProvider;

    private final String secret = "01234567890123456789012345678901234567890123456789012345678901234567890"; 
    private final int expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtTokenProvider = new JwtTokenProvider(
                userDetailsService,
                blacklistService,
                secret,
                expirationMs,
                "ecommerce-app"
        );
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
        // Hazır Authentication objesi 
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Token üretiimi
        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());

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
    		    Mockito.mock(JwtBlacklistService.class),
    		    "1234567890123456789012345678901234567890123456789012345678901234",
    		    1000,
    		    "ecommerce-app"
    		);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = shortLivedProvider.generateToken(authentication);

        // Token süresi doluyor
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
