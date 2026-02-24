package com.example.ecommerce.auth.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final List<String> allowedOrigins;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtValidationService jwtValidationService;

    // Public endpoint listesi
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh-token",
        "/actuator/health"
    );

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   JwtValidationService jwtValidationService,
                                   List<String> allowedOrigins) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtValidationService = jwtValidationService;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        setCorsHeaders(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public endpoint ise filtreyi geç
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = getJwtFromRequest(request);

            if (!StringUtils.hasText(token)) {
                if (isOptionalTokenRequest(request, path)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing JWT token");
                return;
            }

            authenticateRequest(request, token);
        } catch (JwtValidationException e) {
            LOG.warn("JWT validation failed: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        } catch (SignatureException | MalformedJwtException e) {
            LOG.error("Invalid JWT token: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        } catch (ExpiredJwtException e) {
            LOG.warn("Expired JWT token: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token expired");
            return;
        } catch (JwtException e) {
            LOG.error("JWT processing error: {}", e.getMessage());
            clearSecurityContext();
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "JWT processing error");
            return;
        } catch (RuntimeException e) {
            LOG.error("Unexpected error in JWT filter: {}", e.getMessage(), e);
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
        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.contains(path);
    }

    private boolean matchesPathPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private boolean isOptionalTokenRequest(HttpServletRequest request, String path) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return matchesPathPrefix(path, "/api/products") || matchesPathPrefix(path, "/api/cart");
    }

    private void authenticateRequest(HttpServletRequest request, String token) {
        if (!jwtValidationService.validateToken(token)) {
            return;
        }
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.setAttribute("tokenId", jwtTokenProvider.getTokenId(token));
        request.setAttribute("authenticatedUser", auth.getName());
    }
}
