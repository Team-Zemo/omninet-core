package org.zemo.omninet.ai.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    @JsonBackReference
    private ChatSession chatSession;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_file_path")
    private String audioFilePath;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum MessageType {
        USER_TEXT, USER_AUDIO, AI_TEXT, AI_AUDIO
    }

    public ChatMessage(ChatSession chatSession, MessageType type, String content) {
        this.chatSession = chatSession;
        this.type = type;
        this.content = content;
    }
}