package com.example.ecommerce.wishlist.controller;

import com.example.ecommerce.wishlist.dto.WishlistItemResponse;
import com.example.ecommerce.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist", description = "Authenticated wishlist operations")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WISHLIST_READ')")
    @Operation(summary = "List my wishlist", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<WishlistItemResponse>> getMyWishlist() {
        return ResponseEntity.ok(wishlistService.getMyWishlist(getCurrentUsername()));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('WISHLIST_WRITE')")
    @Operation(summary = "Add product to wishlist", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<WishlistItemResponse> addWishlistItem(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.addItem(getCurrentUsername(), productId));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('WISHLIST_WRITE')")
    @Operation(summary = "Remove product from wishlist", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> removeWishlistItem(@PathVariable Long productId) {
        wishlistService.removeItem(getCurrentUsername(), productId);
        return ResponseEntity.noContent().build();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        return authentication.getName();
    }
}
