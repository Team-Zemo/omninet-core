package org.zemo.omninet.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;

    @Column(unique = true)
    private String email;

    private String name;
    private String avatarUrl;
    private String provider;
    private String linkedProviders;
    private boolean accountMerged;

    private String password; // Will be hashed for email registrations
    private boolean emailVerified = false;
    private String registrationSource; // "oauth" or "email"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public User(String id, String email, String name, String provider) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.linkedProviders = provider;
        this.accountMerged = false;
        this.registrationSource = "oauth";
        this.emailVerified = true; // OAuth emails are considered verified
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }

    // Constructor for email registration
    public User(String email, String name, String hashedPassword) {
        this.id = java.util.UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.password = hashedPassword;
        this.provider = "email";
        this.linkedProviders = "email";
        this.accountMerged = false;
        this.registrationSource = "email";
        this.emailVerified = true; // Will be set after OTP verification
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }

    public boolean supportsOAuthProvider(String providerName) {
        return linkedProviders != null && linkedProviders.contains(providerName);
    }
}
