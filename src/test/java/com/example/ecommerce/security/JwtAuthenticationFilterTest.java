package com.example.ecommerce.security;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.ecommerce.auth.security.JwtAuthenticationFilter;
import com.example.ecommerce.auth.security.JwtTokenProvider;

import java.io.PrintWriter;
import java.io.StringWriter;

public class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    public void setup() {
        tokenProvider = mock(JwtTokenProvider.class);
        filter = new JwtAuthenticationFilter(tokenProvider);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @Test
    public void testDoFilterInternal_validToken_setsAuthentication() throws Exception {
        String token = "valid.jwt.token";
        Authentication auth = mock(Authentication.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token)).thenReturn(auth);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_noToken_callsFilterChainOnly() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_invalidSignature_returnsUnauthorized() throws Exception {
        String token = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new SignatureException("Invalid signature")).when(tokenProvider).validateToken(token);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Invalid JWT token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_expiredToken_returnsUnauthorized() throws Exception {
        String token = "expired.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new ExpiredJwtException(null, null, "Token expired")).when(tokenProvider).validateToken(token);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("JWT token expired"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_malformedToken_returnsUnauthorized() throws Exception {
        String token = "malformed.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new MalformedJwtException("Malformed token")).when(tokenProvider).validateToken(token);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("Invalid JWT token"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_jwtException_returnsUnauthorized() throws Exception {
        String token = "jwtException.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new JwtException("JWT error")).when(tokenProvider).validateToken(token);

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseWriter.toString().contains("JWT processing error"));
        verify(filterChain, never()).doFilter(request, response);
    }
    @Test
    public void testDoFilterInternal_unexpectedException_returnsInternalServerError() throws Exception {
        String token = "unexpected.error.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        // getAuthentication metodu RuntimeException fırlatıyor
        when(tokenProvider.getAuthentication(token)).thenThrow(new RuntimeException("Unexpected error"));

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseWriter.toString().contains("Internal server error"));
        verify(filterChain, never()).doFilter(request, response);
    }


}
