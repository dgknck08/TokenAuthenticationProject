package com.example.ecommerce.review.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.exception.ProductNotFoundException;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.review.dto.CreateProductReviewRequest;
import com.example.ecommerce.review.dto.ProductReviewResponse;
import com.example.ecommerce.review.model.ProductReview;
import com.example.ecommerce.review.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProductReviewService {
    private static final Set<OrderStatus> REVIEWABLE_ORDER_STATUSES = EnumSet.of(
            OrderStatus.PAID,
            OrderStatus.PACKED,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern ALLOWED_COMMENT_PATTERN = Pattern.compile("^[\\p{L}\\p{N} ]+$");

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;

    public ProductReviewService(ProductReviewRepository productReviewRepository,
                                ProductRepository productRepository,
                                UserService userService,
                                OrderRepository orderRepository) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        ensureProductExists(productId);
        return productReviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse);
    }

    public ProductReviewResponse createReview(Long productId, String username, CreateProductReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));
        User user = userService.getByUsername(username);

        if (productReviewRepository.existsByProduct_IdAndUserId(productId, user.getId())) {
            throw new IllegalArgumentException("Bu urun icin zaten yorum yaptiniz.");
        }
        if (!orderRepository.hasPurchasedProduct(user.getId(), productId, REVIEWABLE_ORDER_STATUSES)) {
            throw new IllegalArgumentException("Yorum yazabilmek icin urunu satin almis olmaniz gerekir.");
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUserId(user.getId());
        review.setUsername(user.getUsername());
        review.setRating(request.getRating());
        review.setComment(normalizeAndValidateComment(request.getComment()));
        return toResponse(productReviewRepository.save(review));
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Product not found with id " + productId);
        }
    }

    private String normalizeAndValidateComment(String rawComment) {
        String normalized = trimToNull(rawComment);
        if (normalized == null) {
            throw new IllegalArgumentException("Yorum metni zorunludur.");
        }

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        normalized = MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ");
        if (!ALLOWED_COMMENT_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Yorum sadece harf, rakam ve bosluk icerebilir.");
        }
        if (normalized.length() < 10 || normalized.length() > 500) {
            throw new IllegalArgumentException("Yorum 10 ile 500 karakter arasinda olmalidir.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .userId(review.getUserId())
                .username(review.getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
