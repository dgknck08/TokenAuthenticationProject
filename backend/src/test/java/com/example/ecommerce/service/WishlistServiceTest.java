package com.example.ecommerce.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.auth.service.AuditService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.wishlist.model.WishlistItem;
import com.example.ecommerce.wishlist.repository.WishlistItemRepository;
import com.example.ecommerce.wishlist.service.WishlistService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuditService auditService;

    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        wishlistService = new WishlistService(
                wishlistItemRepository,
                userRepository,
                productRepository,
                auditService,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void addItem_shouldCreateWishlistItemWhenNotExists() {
        User user = User.builder().id(3L).username("alice").build();
        Product product = new Product();
        product.setId(5L);
        product.setName("Guitar");
        product.setPrice(new BigDecimal("999.90"));

        WishlistItem saved = new WishlistItem();
        saved.setId(11L);
        saved.setUserId(3L);
        saved.setProduct(product);
        saved.setCreatedAt(Instant.now());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(wishlistItemRepository.findByUserIdAndProduct_Id(3L, 5L)).thenReturn(Optional.empty());
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(saved);

        var response = wishlistService.addItem("alice", 5L);

        assertEquals(11L, response.getId());
        assertEquals(5L, response.getProductId());
        assertEquals("Guitar", response.getProductName());
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void addItem_shouldReturnExistingWishlistItemWhenDuplicate() {
        User user = User.builder().id(4L).username("bob").build();
        Product product = new Product();
        product.setId(7L);
        product.setName("Piano");
        product.setPrice(new BigDecimal("1499.00"));

        WishlistItem existing = new WishlistItem();
        existing.setId(77L);
        existing.setUserId(4L);
        existing.setProduct(product);
        existing.setCreatedAt(Instant.now());

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(wishlistItemRepository.findByUserIdAndProduct_Id(4L, 7L)).thenReturn(Optional.of(existing));

        var response = wishlistService.addItem("bob", 7L);

        assertEquals(77L, response.getId());
        assertEquals(7L, response.getProductId());
        verify(productRepository, never()).findById(any());
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }
}
