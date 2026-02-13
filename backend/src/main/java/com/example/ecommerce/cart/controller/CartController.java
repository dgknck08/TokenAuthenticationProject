package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.ecommerce.auth.service.JwtValidationService;
import com.example.ecommerce.auth.exception.JwtValidationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management for both authenticated and guest users")
public class CartController {

    private final CartService cartService;
    private final JwtValidationService jwtValidationService	;

    @GetMapping
    @Operation(summary = "Get cart", description = "Retrieve cart for authenticated user or guest")
    public ResponseEntity<CartDto> getCart(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.getCartByUserId(userId);
        } else {
            // Guest user
            String sessionId = getOrCreateSessionId(request);
            cart = cartService.getGuestCart(sessionId);
        }
        
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Add a product to cart for authenticated user or guest")
    public ResponseEntity<CartDto> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromRequest(httpRequest);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        } else {
            // Guest user
            String sessionId = getOrCreateSessionId(httpRequest);
            cart = cartService.addItemToGuestCart(sessionId, request.getProductId(), request.getQuantity());
        }
        
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item", description = "Update quantity of a cart item")
    public ResponseEntity<CartDto> updateCartItem(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromRequest(httpRequest);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.updateCartItem(userId, productId, request.getQuantity());
        } else {
            // Guest user
            String sessionId = getOrCreateSessionId(httpRequest);
            cart = cartService.updateGuestCartItem(sessionId, productId, request.getQuantity());
        }
        
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Remove a product from cart")
    public ResponseEntity<CartDto> removeItemFromCart(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromRequest(httpRequest);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.removeItemFromCart(userId, productId);
        } else {
            // Guest user
            String sessionId = getOrCreateSessionId(httpRequest);
            cart = cartService.removeItemFromGuestCart(sessionId, productId);
        }
        
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from cart")
    public ResponseEntity<Void> clearCart(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromRequest(httpRequest);
        
        if (userId != null) {
            // Authenticated user
            cartService.clearCart(userId);
        } else {
            // Guest user
            String sessionId = getOrCreateSessionId(httpRequest);
            cartService.clearGuestCart(sessionId);
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart", description = "Merge guest cart to authenticated user cart after login")
    public ResponseEntity<CartDto> mergeGuestCart(HttpServletRequest httpRequest) {
        Long userId = extractUserIdFromRequest(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        String sessionId = getSessionId(httpRequest);
        if (sessionId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        CartDto mergedCart = cartService.mergeGuestCartToUserCart(sessionId, userId);
        return ResponseEntity.ok(mergedCart);
    }

    // Helper methods
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtValidationService.validateToken(token)) {
                    return jwtValidationService.getUserIdFromToken(token);
                }
                throw new JwtValidationException("Invalid JWT token");
            }
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Could not extract user ID from request", e);
        }
        return null;
    }

    private String getOrCreateSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true); 
        String sessionId = session.getId();
        log.debug("Session ID: {}", sessionId);
        return sessionId;
    }

    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false); 
        return session != null ? session.getId() : null;
    }
}
