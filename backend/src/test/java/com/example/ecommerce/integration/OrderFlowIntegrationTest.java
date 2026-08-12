package com.example.ecommerce.integration;

import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.inventory.service.InventoryService;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String USERNAME = "alice";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private InventoryService inventoryService;

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
    void createOrder_persistsOrderAndDecreasesStock() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(inventoryService.getAvailableStock(productId)).isEqualTo(7);
    }

    @Test
    @WithMockUser(username = USERNAME, authorities = {"ORDER_WRITE", "ORDER_READ"})
    void cancelOrder_restoresStock() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(3)))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertThat(inventoryService.getAvailableStock(productId)).isEqualTo(7);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(inventoryService.getAvailableStock(productId)).isEqualTo(10);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @WithMockUser(username = USERNAME, authorities = "ORDER_READ")
    void createOrder_forbiddenWithoutWriteAuthority() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(1)))
                .andExpect(status().isForbidden());

        assertThat(orderRepository.count()).isZero();
    }

    private String orderBody(int quantity) {
        return """
                {"items":[{"productId":%d,"quantity":%d}]}""".formatted(productId, quantity);
    }
}
