package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactItem {
    private String email;
    private String name;
    private String avatarUrl;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private boolean online;
}
