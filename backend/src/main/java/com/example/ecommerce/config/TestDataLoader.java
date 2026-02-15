package com.example.ecommerce.config;


import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@Component
@Profile("dev")
public class TestDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProductRepository productRepository;

    public TestDataLoader(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            refreshTokenRepository.count();
        }
        seedProductsIfEmpty();
    }

    private void seedProductsIfEmpty() {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.save(buildProduct(
                "Cream Strat Style Electro",
                "Versatile electric guitar with modern playability.",
                new BigDecimal("1299.99"),
                "/products/Cream-Strat-Style-electro.png",
                "electric",
                "Auralyn",
                "STRAT-CREAM-ELEC",
                "cream",
                24
        ));
        productRepository.save(buildProduct(
                "Mat Siyah Acoustic",
                "Balanced acoustic tone for singer-songwriters.",
                new BigDecimal("749.99"),
                "/products/MatSiyah-acustic.png",
                "acoustic",
                "Auralyn",
                "ACOUSTIC-MATTE-BLACK",
                "matte black",
                18
        ));
        productRepository.save(buildProduct(
                "Sunburst Acoustic",
                "Warm resonance with sunburst finish.",
                new BigDecimal("829.99"),
                "/products/Sunburst-acustic.png",
                "acoustic",
                "Auralyn",
                "ACOUSTIC-SUNBURST",
                "sunburst",
                16
        ));
        productRepository.save(buildProduct(
                "Vintage Sunburst Les Paul Style Electro",
                "Thick sustain and classic single-cut feel.",
                new BigDecimal("1499.99"),
                "/products/Vintage-Sunburst-Les-Paul-Style-electro.png",
                "electric",
                "Auralyn",
                "LP-VINTAGE-SUNBURST-ELEC",
                "vintage sunburst",
                14
        ));
    }

    private Product buildProduct(
            String name,
            String description,
            BigDecimal price,
            String imageUrl,
            String category,
            String brand,
            String sku,
            String color,
            int stock
    ) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setCategory(category);
        product.setBrand(brand);
        product.setSku(sku);
        product.setColor(color);
        product.setStock(stock);
        return product;
    }
}

