package com.example.ecommerce.controller;

import com.example.ecommerce.review.controller.ProductReviewController;
import com.example.ecommerce.review.dto.CreateProductReviewRequest;
import com.example.ecommerce.review.dto.ProductReviewResponse;
import com.example.ecommerce.review.service.ProductReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewControllerTest {

    @Mock
    private ProductReviewService productReviewService;

    @InjectMocks
    private ProductReviewController productReviewController;

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProductReviews_shouldReturnOk() {
        ProductReviewResponse response = ProductReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(15L)
                .username("alice")
                .rating(5)
                .comment("Kaliteli urun tavsiye ederim")
                .build();
        when(productReviewService.getProductReviews(10L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(response)));

        ResponseEntity<Page<ProductReviewResponse>> result =
                productReviewController.getProductReviews(10L, PageRequest.of(0, 10));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    void createReview_shouldReturnCreated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        CreateProductReviewRequest request = new CreateProductReviewRequest();
        request.setRating(5);
        request.setComment("Kaliteli urun tavsiye ederim");
        ProductReviewResponse response = ProductReviewResponse.builder()
                .id(1L)
                .productId(10L)
                .userId(15L)
                .username("alice")
                .rating(5)
                .comment("Kaliteli urun tavsiye ederim")
                .build();
        when(productReviewService.createReview(10L, "alice", request)).thenReturn(response);

        ResponseEntity<ProductReviewResponse> result = productReviewController.createReview(10L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().id());
        verify(productReviewService).createReview(10L, "alice", request);
    }
}
