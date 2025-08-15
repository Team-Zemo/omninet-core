package org.zemo.omninet.chat.model;

import jakarta.persistence.*;
import lombok.*;
import org.zemo.omninet.security.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data @NoArgsConstructor @AllArgsConstructor
public class Message {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // FK -> users.id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // FK -> users.id

    @Column(length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, DELIVERED, READ

    private LocalDateTime timestamp = LocalDateTime.now();

    public enum Status { PENDING, DELIVERED, READ }
}
