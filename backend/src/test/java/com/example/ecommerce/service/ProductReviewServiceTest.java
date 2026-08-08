package com.example.ecommerce.service;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.service.UserService;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.review.dto.CreateProductReviewRequest;
import com.example.ecommerce.review.model.ProductReview;
import com.example.ecommerce.review.repository.ProductReviewRepository;
import com.example.ecommerce.review.service.ProductReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void createReview_shouldPersistNormalizedReview_whenUserPurchasedProduct() {
        Product product = new Product();
        product.setId(10L);
        User user = User.builder()
                .id(15L)
                .username("alice")
                .password("pw")
                .email("alice@test.com")
                .firstName("Alice")
                .lastName("Doe")
                .build();
        CreateProductReviewRequest request = new CreateProductReviewRequest();
        request.setRating(5);
        request.setComment("  Cok  kaliteli  urun  123  ");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userService.getByUsername("alice")).thenReturn(user);
        when(productReviewRepository.existsByProduct_IdAndUserId(10L, 15L)).thenReturn(false);
        when(orderRepository.hasPurchasedProduct(eq(15L), eq(10L), eq(EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED
        )))).thenReturn(true);
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        var response = productReviewService.createReview(10L, "alice", request);

        assertEquals(99L, response.id());
        assertEquals("Cok kaliteli urun 123", response.comment());
        assertEquals(5, response.rating());

        ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
        verify(productReviewRepository).save(captor.capture());
        assertEquals("alice", captor.getValue().getUsername());
        assertEquals(15L, captor.getValue().getUserId());
    }

    @Test
    void createReview_shouldRejectDuplicateReview() {
        Product product = new Product();
        product.setId(10L);
        User user = User.builder()
                .id(15L)
                .username("alice")
                .password("pw")
                .email("alice@test.com")
                .firstName("Alice")
                .lastName("Doe")
                .build();
        CreateProductReviewRequest request = new CreateProductReviewRequest();
        request.setRating(4);
        request.setComment("Kaliteli ve kullanisli urun");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userService.getByUsername("alice")).thenReturn(user);
        when(productReviewRepository.existsByProduct_IdAndUserId(10L, 15L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productReviewService.createReview(10L, "alice", request));

        assertEquals("Bu urun icin zaten yorum yaptiniz.", exception.getMessage());
    }

    @Test
    void createReview_shouldRejectCommentWithSpecialCharacters() {
        Product product = new Product();
        product.setId(10L);
        User user = User.builder()
                .id(15L)
                .username("alice")
                .password("pw")
                .email("alice@test.com")
                .firstName("Alice")
                .lastName("Doe")
                .build();
        CreateProductReviewRequest request = new CreateProductReviewRequest();
        request.setRating(4);
        request.setComment("Harika <script>");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userService.getByUsername("alice")).thenReturn(user);
        when(productReviewRepository.existsByProduct_IdAndUserId(10L, 15L)).thenReturn(false);
        when(orderRepository.hasPurchasedProduct(any(), any(), any())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productReviewService.createReview(10L, "alice", request));

        assertEquals("Yorum sadece harf, rakam ve bosluk icerebilir.", exception.getMessage());
    }

    @Test
    void createReview_shouldRejectWhenProductWasNotPurchased() {
        Product product = new Product();
        product.setId(10L);
        User user = User.builder()
                .id(15L)
                .username("alice")
                .password("pw")
                .email("alice@test.com")
                .firstName("Alice")
                .lastName("Doe")
                .build();
        CreateProductReviewRequest request = new CreateProductReviewRequest();
        request.setRating(4);
        request.setComment("Kaliteli ve kullanisli urun");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userService.getByUsername("alice")).thenReturn(user);
        when(productReviewRepository.existsByProduct_IdAndUserId(10L, 15L)).thenReturn(false);
        when(orderRepository.hasPurchasedProduct(any(), any(), any())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> productReviewService.createReview(10L, "alice", request));

        assertEquals("Yorum yazabilmek icin urunu satin almis olmaniz gerekir.", exception.getMessage());
    }

    @Test
    void getProductReviews_shouldReturnPage() {
        ProductReview review = new ProductReview();
        Product product = new Product();
        product.setId(10L);
        review.setId(1L);
        review.setProduct(product);
        review.setUserId(15L);
        review.setUsername("alice");
        review.setRating(5);
        review.setComment("Kaliteli urun tavsiye ederim");

        when(productRepository.existsById(10L)).thenReturn(true);
        when(productReviewRepository.findByProduct_IdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review)));

        var result = productReviewService.getProductReviews(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("alice", result.getContent().get(0).username());
    }
}
