package com.example.ecommerce.auth.model;

import java.time.Instant;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    @Column(length = 50)
    private String username;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    @Column(length = 255)
    private String description;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON format 
    
    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;
    
    public enum AuditAction {
        USER_REGISTRATION,
        USER_LOGIN_SUCCESS,
        USER_LOGIN_FAILURE,
        USER_LOGOUT,
        USER_ACCOUNT_LOCKED,
        USER_ACCOUNT_UNLOCKED,
        PASSWORD_CHANGED,
        TOKEN_REFRESH,
        TOKEN_BLACKLISTED,
        PROFILE_UPDATED,
        EMAIL_VERIFICATION,
        SUSPICIOUS_ACTIVITY
    }
}
