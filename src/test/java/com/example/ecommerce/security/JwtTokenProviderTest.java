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
import com.example.ecommerce.auth.security.JwtUtils;
import com.github.benmanes.caffeine.cache.Cache;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import java.util.List;

class JwtTokenProviderTest {

	 @Mock
	    private UserDetailsService userDetailsService;

	    @Mock
	    private JwtUtils jwtUtils;

	    @Mock
	    private Cache<String, Claims> jwtClaimsCache; // Burayı Claims tipinde yap

	    @Mock
	    private Cache<String, Boolean> jwtValidationCache;

	    @Mock
	    private Cache<String, UserDetails> userDetailsCache; // Burayı UserDetails tipinde yap

	    private JwtTokenProvider jwtTokenProvider;

	    private final String secret = "01234567890123456789012345678901234567890123456789012345678901234567890";
	    private final int expirationMs = 3600000;

	    @BeforeEach
	    void setUp() {
	        MockitoAnnotations.openMocks(this);
	        jwtTokenProvider = new JwtTokenProvider(
	            userDetailsService,
	            jwtUtils,
	            jwtClaimsCache,          // Artık cast yok, tipi doğru
	            jwtValidationCache,
	            userDetailsCache,        // Doğru tipte mock
	            secret,
	            expirationMs,
	            "ecommerce-app"
	        );
	    }

    @Test
    void testGenerateTokenWithUsername_simple() {
        String username = "user1";
        when(jwtUtils.parseToken(anyString())).thenReturn(mock(io.jsonwebtoken.Claims.class));
        String token = jwtTokenProvider.generateTokenWithUsername(username);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateTokenWithUsername_withRoles() {
        String username = "user1";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");
        when(jwtUtils.parseToken(anyString())).thenReturn(mock(io.jsonwebtoken.Claims.class));
        String token = jwtTokenProvider.generateTokenWithUsername(username, roles);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetAuthentication() {
        UserDetails userDetails = new User("authUser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("authUser")).thenReturn(userDetails);

        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("authUser");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));
        when(jwtClaimsCache.getIfPresent(anyString())).thenReturn(claims);

        Authentication authentication = jwtTokenProvider.getAuthentication("token");

        assertNotNull(authentication);
        assertEquals("authUser", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    public void testGenerateToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        when(jwtUtils.parseToken(anyString())).thenReturn(mock(io.jsonwebtoken.Claims.class));

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertNotNull(username);
    }

    @Test
    void testValidateTokenStructure_ValidToken() {
        when(jwtUtils.parseToken(anyString())).thenReturn(mock(io.jsonwebtoken.Claims.class));
        when(jwtValidationCache.get(anyString(), any())).thenCallRealMethod();

        boolean valid = jwtTokenProvider.validateTokenStructure("validToken");

        assertTrue(valid);
    }

    @Test
    void testValidateTokenStructure_ExpiredToken() {
        when(jwtValidationCache.get(anyString(), any())).then(invocation -> {
            throw new ExpiredJwtException(null, null, "Token expired");
        });

        assertThrows(JwtValidationException.class, () -> jwtTokenProvider.validateTokenStructure("expiredToken"));
    }

    @Test
    void testValidateTokenStructure_InvalidToken() {
        when(jwtValidationCache.get(anyString(), any())).then(invocation -> {
            throw new JwtValidationException("Invalid token");
        });

        assertThrows(JwtValidationException.class, () -> jwtTokenProvider.validateTokenStructure("invalidToken"));
    }
}
