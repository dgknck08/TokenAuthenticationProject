package com.example.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Entegrasyon testlerinin ortak temeli.
 *
 * Tüm context'i ayağa kaldırıp isteği gerçek zincirde
 * (controller -> service -> repository) çalıştırır. Veritabanı, Testcontainers
 * ile açılan gerçek bir PostgreSQL; şemayı Hibernate üretir. Hiçbir katman mock değil.
 *
 * Güvenlik filtresi kapalı (addFilters = false), çünkü JWT filtresi token'sız
 * isteği controller'a varmadan reddeder. @PreAuthorize yine çalışır, o yüzden
 * yetkiyi @WithMockUser ile test ederiz.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // Tek container'ı bütün testler paylaşır. Sınıflar arası kapatmıyoruz;
    // context tekrar kullanıldığından kapatırsak datasource bozulur.
    // JVM kapanınca Testcontainers kendisi temizler.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Her testten önce tüm tabloları boşaltır; paylaşılan container'da her test
     * temiz başlasın diye. Üst sınıfın @BeforeEach'i alt sınıftakinden önce koşar,
     * yani seed'lerden önce temizler. CASCADE, foreign key sırası derdini bitirir.
     */
    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        if (tables.isEmpty()) {
            return;
        }
        String joined = tables.stream()
                .map(table -> "\"" + table + "\"")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
    }
}
