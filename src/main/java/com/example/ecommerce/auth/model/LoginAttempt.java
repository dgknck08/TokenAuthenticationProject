package com.example.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    @Column(length = 45) 
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(nullable = false)
    private boolean successful;
    
    @Column(length = 255)
    private String failureReason;
    
    @Column(length = 100)
    private String location; // Şehir/Ülke bilgisi
    
    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;
    
    // Login attempt sonrasi durum
    private boolean accountLocked;
    private int attemptCount; //toplam deneme sayisi
}