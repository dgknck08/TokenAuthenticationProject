package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.AddToCartRequest;
import com.example.ecommerce.cart.dto.CartDto;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.auth.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management for both authenticated and guest users")
public class CartController {

    private final CartService cartService;
    private static final String GUEST_CART_COOKIE_NAME = "guest_cart_id";
    private static final Duration GUEST_CART_COOKIE_TTL = Duration.ofDays(30);
    private final boolean cookieSecure;

    public CartController(CartService cartService, @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.cartService = cartService;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping
    @Operation(summary = "Get cart", description = "Retrieve cart for authenticated user or guest")
    public ResponseEntity<CartDto> getCart(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.getCartByUserId(userId);
        } else {
            // Guest user
            String guestCartId = getOrCreateGuestCartId(request, response);
            cart = cartService.getGuestCart(guestCartId);
        }
        
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Add a product to cart for authenticated user or guest")
    public ResponseEntity<CartDto> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        } else {
            // Guest user
            String guestCartId = getOrCreateGuestCartId(httpRequest, httpResponse);
            cart = cartService.addItemToGuestCart(guestCartId, request.getProductId(), request.getQuantity());
        }
        
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item", description = "Update quantity of a cart item")
    public ResponseEntity<CartDto> updateCartItem(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.updateCartItem(userId, productId, request.getQuantity());
        } else {
            // Guest user
            String guestCartId = getOrCreateGuestCartId(httpRequest, httpResponse);
            cart = cartService.updateGuestCartItem(guestCartId, productId, request.getQuantity());
        }
        
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Remove a product from cart")
    public ResponseEntity<CartDto> removeItemFromCart(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        CartDto cart;
        
        if (userId != null) {
            // Authenticated user
            cart = cartService.removeItemFromCart(userId, productId);
        } else {
            // Guest user
            String guestCartId = getOrCreateGuestCartId(httpRequest, httpResponse);
            cart = cartService.removeItemFromGuestCart(guestCartId, productId);
        }
        
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from cart")
    public ResponseEntity<Void> clearCart(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        
        if (userId != null) {
            // Authenticated user
            cartService.clearCart(userId);
        } else {
            // Guest user
            String guestCartId = getOrCreateGuestCartId(httpRequest, httpResponse);
            cartService.clearGuestCart(guestCartId);
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart", description = "Merge guest cart to authenticated user cart after login")
    public ResponseEntity<CartDto> mergeGuestCart(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        String guestCartId = getExistingGuestCartId(httpRequest);
        if (guestCartId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        CartDto mergedCart = cartService.mergeGuestCartToUserCart(guestCartId, userId);
        clearGuestCartCookie(httpResponse);
        return ResponseEntity.ok(mergedCart);
    }

    // Helper methods
    private Long extractUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser().getId();
        }

        log.debug("Authentication principal is not CustomUserDetails: {}", principal);
        return null;
    }

    private String getOrCreateGuestCartId(HttpServletRequest request, HttpServletResponse response) {
        String existing = getExistingGuestCartId(request);
        if (existing != null) {
            return existing;
        }

        String guestCartId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(GUEST_CART_COOKIE_NAME, guestCartId)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(GUEST_CART_COOKIE_TTL)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Generated guest_cart_id cookie: {}", guestCartId);
        return guestCartId;
    }

    private String getExistingGuestCartId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (GUEST_CART_COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearGuestCartCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(GUEST_CART_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
