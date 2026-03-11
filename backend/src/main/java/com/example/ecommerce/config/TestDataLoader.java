package com.example.ecommerce.config;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


import com.example.ecommerce.auth.repository.RefreshTokenRepository;
import com.example.ecommerce.auth.repository.UserRepository;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.ProductRepository;

@Component
@Profile({"dev", "docker"})
public class TestDataLoader implements CommandLineRunner {
    private static final List<String> LEGACY_SEED_SKUS = List.of(
            "STRAT-CREAM-ELEC",
            "ACOUSTIC-MATTE-BLACK",
            "ACOUSTIC-SUNBURST",
            "LP-VINTAGE-SUNBURST-ELEC"
    );

    private static final Map<String, String> CLOUD_IMAGES = Map.ofEntries(
            Map.entry("electric-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113195/DM-GTR-ELEC-001_mz6mse.png"),
            Map.entry("electric-2", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113194/DM-GTR-ELEC-002_ns24mg.png"),
            Map.entry("acoustic-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113193/DM-GTR-ACS-001_n1kttk.png"),
            Map.entry("acoustic-2", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113194/DM-GTR-ACS-002_mk7dxq.png"),
            Map.entry("bass-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113192/DM-GTR-BASS-001_mcvxa6.png"),
            Map.entry("amp-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113199/DM-AMP-001_wixnop.png"),
            Map.entry("pedal-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113196/DM-PDL-001_g2qff3.png"),
            Map.entry("drum-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113193/DM-DRM-001_nu5zlh.png"),
            Map.entry("drum-2", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113195/DM-DRM-002_jf8p78.png"),
            Map.entry("keyboard-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113193/DM-KEY-001_mwqfca.png"),
            Map.entry("keyboard-2", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113195/DM-KEY-002_xblexj.png"),
            Map.entry("studio-mic", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113197/DM-STD-001_z3wktn.png"),
            Map.entry("studio-interface", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113197/DM-STD-002_im5nkb.png"),
            Map.entry("studio-headphones", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113197/DM-STD-003_d1z7fw.png"),
            Map.entry("accessory-strings", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113198/DM-ACC-001_uvsqga.png"),
            Map.entry("accessory-bag", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113199/DM-ACC-002_dubx5p.png"),
            Map.entry("bundle-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113200/DM-BND-001_ybcuix.png"),
            Map.entry("campaign-1", "https://res.cloudinary.com/dbtoykeu4/image/upload/v1772113201/DM-CMP-001_trnrrl.png")
    );

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
        cleanupLegacySeedProducts();
        seedOrUpdateProducts();
    }

    private void cleanupLegacySeedProducts() {
        try {
            productRepository.deleteBySkuIn(LEGACY_SEED_SKUS);
        } catch (Exception ignored) {
            // Do not fail application startup for legacy cleanup.
        }
    }

    private void seedOrUpdateProducts() {
        List<SeedProduct> products = List.of(
                new SeedProduct("Stage Pro Strat Electric", "Modern electric guitar for rock and metal players.", new BigDecimal("14999.00"), CLOUD_IMAGES.get("electric-1"), "electric", "Fender", "DM-GTR-ELEC-001", "cream", 18),
                new SeedProduct("Vintage LP Electric", "Classic single-cut electric guitar with warm sustain for blues and jazz.", new BigDecimal("16999.00"), CLOUD_IMAGES.get("electric-2"), "electric", "Gibson", "DM-GTR-ELEC-002", "sunburst", 12),
                new SeedProduct("Beginner Acoustic Set", "Starter acoustic guitar set for new musicians.", new BigDecimal("7999.00"), CLOUD_IMAGES.get("acoustic-1"), "acoustic", "Yamaha", "DM-GTR-ACS-001", "matte black", 20),
                new SeedProduct("Performance Acoustic Sunburst", "Balanced acoustic guitar for stage and home practice.", new BigDecimal("9499.00"), CLOUD_IMAGES.get("acoustic-2"), "acoustic", "Takamine", "DM-GTR-ACS-002", "sunburst", 15),
                new SeedProduct("Studio Bass 4-String", "Bass guitar with tight low-end for recording and live sessions.", new BigDecimal("13999.00"), CLOUD_IMAGES.get("bass-1"), "bass", "Ibanez", "DM-GTR-BASS-001", "black", 9),
                new SeedProduct("Tube Combo Amp 30W", "Combo amp with warm overdrive and reverb for electric guitar.", new BigDecimal("11499.00"), CLOUD_IMAGES.get("amp-1"), "amp", "Marshall", "DM-AMP-001", "black", 11),
                new SeedProduct("Pedalboard Drive Bundle", "Overdrive, delay and reverb pedal bundle for live tone shaping.", new BigDecimal("6599.00"), CLOUD_IMAGES.get("pedal-1"), "pedal", "Boss", "DM-PDL-001", "mixed", 25),
                new SeedProduct("Hybrid Electronic Drum Kit", "Electronic drum set with mesh pads for silent home practice.", new BigDecimal("21999.00"), CLOUD_IMAGES.get("drum-1"), "drum", "Roland", "DM-DRM-001", "black", 6),
                new SeedProduct("Acoustic Drum Pack", "Acoustic drum kit for studio and rehearsal room.", new BigDecimal("25999.00"), CLOUD_IMAGES.get("drum-2"), "drum", "Pearl", "DM-DRM-002", "blue", 4),
                new SeedProduct("88-Key Digital Piano", "Weighted-key digital piano for beginner and pro practice.", new BigDecimal("28999.00"), CLOUD_IMAGES.get("keyboard-1"), "keyboard", "Yamaha", "DM-KEY-001", "black", 7),
                new SeedProduct("MIDI Keyboard Controller", "49-key MIDI controller for beatmaking and home studio production.", new BigDecimal("6299.00"), CLOUD_IMAGES.get("keyboard-2"), "keyboard", "Novation", "DM-KEY-002", "black", 16),
                new SeedProduct("Condenser Studio Microphone", "Large-diaphragm microphone for vocal and instrument recording.", new BigDecimal("4599.00"), CLOUD_IMAGES.get("studio-mic"), "studio", "Audio-Technica", "DM-STD-001", "black", 22),
                new SeedProduct("USB Audio Interface 2x2", "Low-latency audio interface for home studio recording.", new BigDecimal("5499.00"), CLOUD_IMAGES.get("studio-interface"), "studio", "Focusrite", "DM-STD-002", "red", 19),
                new SeedProduct("Closed-Back Studio Headphones", "Monitoring headphones for mixing and tracking sessions.", new BigDecimal("2799.00"), CLOUD_IMAGES.get("studio-headphones"), "studio", "AKG", "DM-STD-003", "black", 30),
                new SeedProduct("Guitar Strings Value Pack", "Nickel guitar strings multipack for electric and acoustic guitars.", new BigDecimal("799.00"), CLOUD_IMAGES.get("accessory-strings"), "accessory", "D'Addario", "DM-ACC-001", "silver", 80),
                new SeedProduct("Premium Guitar Gig Bag", "Padded gig bag for safe transport to rehearsal and stage.", new BigDecimal("1299.00"), CLOUD_IMAGES.get("accessory-bag"), "accessory", "Fender", "DM-ACC-002", "black", 45),
                new SeedProduct("Studio Build Bundle", "Audio interface, microphone and headphones bundle for studio setup.", new BigDecimal("10999.00"), CLOUD_IMAGES.get("bundle-1"), "studio", "Yamaha", "DM-BND-001", "mixed", 10),
                new SeedProduct("Weekend Deal Electric Pack", "Campaign pack: electric guitar + combo amp + cable.", new BigDecimal("22999.00"), CLOUD_IMAGES.get("campaign-1"), "campaign", "Fender", "DM-CMP-001", "mixed", 8)
        );

        for (SeedProduct seed : products) {
            Product product = productRepository.findBySku(seed.sku())
                    .map(existing -> applySeed(existing, seed))
                    .orElseGet(() -> buildProduct(
                            seed.name(),
                            seed.description(),
                            seed.price(),
                            seed.imageUrl(),
                            seed.category(),
                            seed.brand(),
                            seed.sku(),
                            seed.color(),
                            seed.stock()
                    ));
            productRepository.save(product);
        }
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

    private Product applySeed(Product product, SeedProduct seed) {
        product.setName(seed.name());
        product.setDescription(seed.description());
        product.setPrice(seed.price());
        product.setImageUrl(seed.imageUrl());
        product.setCategory(seed.category());
        product.setBrand(seed.brand());
        product.setSku(seed.sku());
        product.setColor(seed.color());
        product.setStock(seed.stock());
        return product;
    }

    private record SeedProduct(
            String name,
            String description,
            BigDecimal price,
            String imageUrl,
            String category,
            String brand,
            String sku,
            String color,
            int stock
    ) {}
}

