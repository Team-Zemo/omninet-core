package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageView {
    private String id;
    private String senderEmail;
    private String receiverEmail;
    private String content;
    private LocalDateTime timestamp;
    private String status;
}
