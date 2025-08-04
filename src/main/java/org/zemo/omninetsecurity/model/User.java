package org.zemo.omninetsecurity.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

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
    private String primaryProvider;
    private String linkedProviders;
    private boolean accountMerged;
    private String mergedFromUserId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Transient
    private Map<String, Object> attributes;

    public User(String id, String email, String name, String provider) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.primaryProvider = provider;
        this.linkedProviders = provider;
        this.accountMerged = false;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }
}
