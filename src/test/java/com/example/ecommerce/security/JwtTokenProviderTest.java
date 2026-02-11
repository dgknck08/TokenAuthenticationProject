package com.example.ecommerce.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

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
    private Cache<String, Claims> jwtClaimsCache;

    @Mock
    private Cache<String, Boolean> jwtValidationCache;

    @Mock
    private Cache<String, UserDetails> userDetailsCache;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        jwtTokenProvider = new JwtTokenProvider(
            userDetailsService,
            jwtUtils,
            jwtClaimsCache,
            jwtValidationCache,
            userDetailsCache,
            3600000,
            "ecommerce-app"
        );

        doNothing().when(jwtClaimsCache).put(anyString(), any());
    }

    @Test
    void testGenerateTokenWithUsername_simple() {
        String username = "user1";

        Claims claimsMock = mock(Claims.class);
        when(jwtUtils.parseToken(anyString())).thenReturn(claimsMock);
        doNothing().when(jwtClaimsCache).put(anyString(), any());

        String token = jwtTokenProvider.generateTokenWithUsername(username);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetAuthentication() {
        UserDetails userDetails = new User("authUser", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("authUser")).thenReturn(userDetails);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("authUser");
        when(claims.get("roles")).thenReturn(List.of("ROLE_USER"));

        when(jwtClaimsCache.getIfPresent("token")).thenReturn(claims);
        when(userDetailsCache.get(eq("authUser"), any())).thenReturn(userDetails);

        Authentication authentication = jwtTokenProvider.getAuthentication("token");

        assertNotNull(authentication);
        assertEquals("authUser", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void testValidateTokenStructure_ValidToken() {
        when(jwtValidationCache.get(anyString(), any())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            java.util.function.Function<String, Boolean> func = invocation.getArgument(1);
            return func.apply(token);
        });

        when(jwtUtils.parseToken(anyString())).thenReturn(mock(Claims.class));

        boolean valid = jwtTokenProvider.validateTokenStructure("validToken");

        assertTrue(valid);
    }

    @Test
    void testValidateTokenStructure_ExpiredToken() {
        when(jwtValidationCache.get(anyString(), any())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            java.util.function.Function<String, Boolean> func = invocation.getArgument(1);

            when(jwtUtils.parseToken(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

            return func.apply(token);
        });

        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> {
            jwtTokenProvider.validateTokenStructure("expiredToken");
        });

        assertTrue(ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void testValidateTokenStructure_InvalidToken() {
        when(jwtValidationCache.get(anyString(), any())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            java.util.function.Function<String, Boolean> func = invocation.getArgument(1);

            when(jwtUtils.parseToken(token)).thenThrow(new JwtValidationException("Invalid token"));

            return func.apply(token);
        });

        assertThrows(JwtValidationException.class, () -> {
            jwtTokenProvider.validateTokenStructure("invalidToken");
        });
    }
}
