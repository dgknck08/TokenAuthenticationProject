package com.example.ecommerce.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import com.example.ecommerce.auth.enums.Role;
import com.example.ecommerce.cart.model.Cart;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Builder.Default
    private boolean enabled = true;

    //Account Lockout
    @Builder.Default
    private boolean accountLocked = false;
    
    private Instant lockedUntil;
    
    @Builder.Default
    private int failedLoginAttempts = 0;
    
    private Instant lastFailedLogin;
    
    private Instant lastSuccessfulLogin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cart cart;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RefreshToken refreshToken;

    //Helpers
    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return roles.stream().anyMatch(Role::isAdmin);
    }

    public boolean canModerate() {
        return roles.stream().anyMatch(Role::canModerate);
    }
    
    // Account Lockout methodlari
    public boolean isAccountNonLocked() {
        if (!accountLocked) return true;
        
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            accountLocked = false;
            lockedUntil = null;
            failedLoginAttempts = 0;
            return true;
        }
        
        return false;
    }
    
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = Instant.now();
    }
    
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLogin = null;
        this.lastSuccessfulLogin = Instant.now();
    }
    
    public void lockAccount(Instant lockUntil) {
        this.accountLocked = true;
        this.lockedUntil = lockUntil;
    }
}