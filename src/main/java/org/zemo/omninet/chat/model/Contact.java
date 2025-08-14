package org.zemo.omninet.chat.model;

import jakarta.persistence.*;
import lombok.*;
import org.zemo.omninet.security.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "contact_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // the one who has this contact entry

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private User contact; // the person they can chat with

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt; // convenience for ordering list
}