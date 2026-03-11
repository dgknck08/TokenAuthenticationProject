package com.example.ecommerce.wishlist.service;

import com.example.ecommerce.auth.model.AuditLog;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.wishlist.dto.WishlistItemResponse;
import com.example.ecommerce.wishlist.model.WishlistItem;
import com.example.ecommerce.wishlist.repository.WishlistItemRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class WishlistService {
    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public WishlistService(WishlistItemRepository wishlistItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           AuditService auditService,
                           MeterRegistry meterRegistry) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist(String username) {
        User user = getUserByUsername(username);
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public WishlistItemResponse addItem(String username, Long productId) {
        long startNanos = System.nanoTime();
        User user = getUserByUsername(username);

        WishlistItem existing = wishlistItemRepository.findByUserIdAndProduct_Id(user.getId(), productId)
                .orElse(null);
        if (existing != null) {
            recordMetric("add", "duplicate", startNanos);
            return toResponse(existing);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        WishlistItem item = new WishlistItem();
        item.setUserId(user.getId());
        item.setProduct(product);
        WishlistItem saved = wishlistItemRepository.save(item);
        recordMetric("add", "success", startNanos);
        auditService.logSystemEvent(
                user.getId(),
                username,
                AuditLog.AuditAction.WISHLIST_ITEM_ADDED,
                "Product added to wishlist",
                Map.of("productId", productId, "wishlistItemId", saved.getId())
        );
        return toResponse(saved);
    }

    public void removeItem(String username, Long productId) {
        long startNanos = System.nanoTime();
        User user = getUserByUsername(username);
        long deletedCount = wishlistItemRepository.deleteByUserIdAndProduct_Id(user.getId(), productId);
        recordMetric("remove", deletedCount > 0 ? "success" : "noop", startNanos);
        if (deletedCount > 0) {
            auditService.logSystemEvent(
                    user.getId(),
                    username,
                    AuditLog.AuditAction.WISHLIST_ITEM_REMOVED,
                    "Product removed from wishlist",
                    Map.of("productId", productId)
            );
        }
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();
        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productImageUrl(product.getImageUrl())
                .productCategory(product.getCategory())
                .productBrand(product.getBrand())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private void recordMetric(String action, String outcome, long startNanos) {
        meterRegistry.counter("ecommerce.wishlist.events", "action", action, "outcome", outcome).increment();
        Timer.builder("ecommerce.wishlist.action.duration")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
