package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.security.JwtTokenProvider;
import com.example.ecommerce.auth.service.JwtValidationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
public class CacheManagementController {
	
    private final JwtValidationService jwtValidationService;
    private final JwtTokenProvider jwtTokenProvider;

    public CacheManagementController(JwtValidationService jwtValidationService,
                                     JwtTokenProvider jwtTokenProvider) {
        this.jwtValidationService = jwtValidationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = jwtTokenProvider.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/clear/{username}")
    @PreAuthorize("hasAuthority('AUDIT_WRITE')")
    public ResponseEntity<Map<String, String>> clearUserCache(@PathVariable String username) {
    	jwtValidationService.invalidateUserTokens(username);
        return ResponseEntity.ok(Map.of("message", "Cache cleared for user: " + username));
    }

    @PostMapping("/clear/all")
    @PreAuthorize("hasAuthority('AUDIT_WRITE')")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        jwtTokenProvider.invalidateAllCaches();
        return ResponseEntity.ok(Map.of("message", "All caches cleared"));
    }
}
