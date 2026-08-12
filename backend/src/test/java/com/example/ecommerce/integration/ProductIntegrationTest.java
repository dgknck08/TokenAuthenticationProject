package com.example.ecommerce.integration;

import com.example.ecommerce.inventory.model.InventoryItem;
import com.example.ecommerce.inventory.repository.InventoryRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @WithMockUser(authorities = "PRODUCT_WRITE")
    void createProduct_persistsProductAndInitializesInventoryAtomically() throws Exception {
        String body = """
                {"name":"Fender Strat","description":"Electric guitar for the stage",
                 "price":14999.00,"category":"Electric","stock":8}""";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fender Strat"));

        assertThat(productRepository.count()).isEqualTo(1);

        Long productId = productRepository.findAll().get(0).getId();
        InventoryItem inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(inventory.getAvailableStock()).isEqualTo(8);
    }

    @Test
    @WithMockUser(authorities = "PRODUCT_WRITE")
    void createProduct_rejectsInvalidPayloadWithValidationError() throws Exception {
        String missingRequiredFields = """
                {"name":"","price":10.00}""";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingRequiredFields))
                .andExpect(status().isBadRequest());

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void getAllProducts_returnsOnlyActiveProductsFromDatabase() throws Exception {
        productRepository.save(activeProduct("Active Guitar", "Electric"));
        Product inactive = activeProduct("Retired Guitar", "Electric");
        inactive.setActive(false);
        productRepository.save(inactive);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Active Guitar"));
    }

    @Test
    void searchProducts_findsSeededProductByQueryAgainstRealDatabase() throws Exception {
        productRepository.save(activeProduct("Vintage Telecaster", "Electric"));
        productRepository.save(activeProduct("Acoustic Dreadnought", "Acoustic"));

        mockMvc.perform(get("/api/products/search").param("q", "Telecaster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Vintage Telecaster"));
    }

    private Product activeProduct(String name, String category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal("999.90"));
        product.setCategory(category);
        product.setStock(5);
        product.setActive(true);
        return product;
    }
}
