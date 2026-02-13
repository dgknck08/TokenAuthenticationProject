package com.example.ecommerce.auth.model;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_token_hash", columnList = "token_hash"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiryDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public RefreshToken(String tokenHash, User user, Instant expiryDate) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());
    }
}
