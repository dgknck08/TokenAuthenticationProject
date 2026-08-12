package com.example.ecommerce.integration;

import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.order.dto.CreateOrderRequest;
import com.example.ecommerce.order.dto.OrderItemRequest;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewIntegrationTest extends AbstractIntegrationTest {

    private static final String USERNAME = "alice";
    private static final String REVIEW_BODY = """
            {"rating":5,"comment":"Great guitar with a warm tone"}""";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductReviewRepository productReviewRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private OrderService orderService;

    private Long productId;

    @BeforeEach
    void setup() {
        userRepository.save(User.builder()
                .username(USERNAME)
                .password("hashed-password")
                .email("alice@test.com")
                .emailVerified(true)
                .firstName("Alice")
                .lastName("Doe")
                .enabled(true)
                .roles(Set.of(Role.ROLE_USER))
                .build());

        Product product = new Product();
        product.setName("Stage Guitar");
        product.setDescription("Reliable stage guitar");
        product.setPrice(new BigDecimal("1000.00"));
        product.setCategory("Electric");
        product.setStock(10);
        product.setActive(true);
        productId = productRepository.save(product).getId();
        inventoryService.initializeStock(productId, 10);
    }

    @Test
    @WithMockUser(username = USERNAME, authorities = "ORDER_WRITE")
    void createReview_rejectedWhenUserHasNotPurchasedProduct() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_BODY))
                .andExpect(status().isBadRequest());

        assertThat(productReviewRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = USERNAME, authorities = "ORDER_WRITE")
    void createReview_succeedsAfterPurchase() throws Exception {
        placePaidOrderForProduct();

        mockMvc.perform(post("/api/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));

        assertThat(productReviewRepository.count()).isEqualTo(1);
    }

    private void placePaidOrderForProduct() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(item));

        Long orderId = orderService.createOrder(USERNAME, request).getId();
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }
}
