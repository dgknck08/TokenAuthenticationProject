package com.example.ecommerce.auth.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ecommerce.auth.exception.JwtValidationException;
import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.common.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final List<String> ALLOWED_ORIGINS = List.of(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://frontend"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtValidationService jwtValidationService;

    // Public endpoint listesi
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token"
    );

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, 
                                   JwtValidationService jwtValidationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        setCorsHeaders(request, response);

        // Preflight OPTIONS isteğini geç
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
            return;
        }

        // Public endpoint ise filtreyi geç
        String path = request.getRequestURI();
        if (PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = getJwtFromRequest(request);

            if (StringUtils.hasText(token)) {
                if (jwtValidationService.validateToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    String tokenId = jwtTokenProvider.getTokenId(token);
                    request.setAttribute("tokenId", tokenId);
                    request.setAttribute("authenticatedUser", auth.getName());
                }
            } else {
                // Sadece public read endpointleri token olmadan geç
                boolean isOptional =
                    ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/products"))
                        || ("GET".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/cart"));
                if (isOptional) {
                    filterChain.doFilter(request, response);
                    return;
                }
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing JWT token");
            return;
        }
    } catch (JwtValidationException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        } catch (SignatureException | MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token expired");
            return;
        } catch (JwtException e) {
            logger.error("JWT processing error: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "JWT processing error");
            return;
        } catch (RuntimeException e) {
            logger.error("Unexpected error in JWT filter: {}", e.getMessage(), e);
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        setCorsHeaders(request, response);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        String code = switch (status) {
            case HttpServletResponse.SC_UNAUTHORIZED -> "UNAUTHORIZED";
            case HttpServletResponse.SC_FORBIDDEN -> "ACCESS_DENIED";
            case HttpServletResponse.SC_BAD_REQUEST -> "BAD_REQUEST";
            case HttpServletResponse.SC_INTERNAL_SERVER_ERROR -> "INTERNAL_SERVER_ERROR";
            default -> "REQUEST_FAILED";
        };
        String json = OBJECT_MAPPER.writeValueAsString(
                ApiErrorResponse.of(code, message, request.getRequestURI())
        );
        response.getWriter().write(json);
    }

    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }
}
