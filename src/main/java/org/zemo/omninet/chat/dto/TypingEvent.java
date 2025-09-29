package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {

    @Email(message = "Invalid from email format")
    private String fromEmail;

    @NotBlank(message = "To email cannot be blank")
    @Email(message = "Invalid to email format")
    private String toEmail;

    private boolean typing;

    // Sanitize emails
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail != null ? fromEmail.trim().toLowerCase() : null;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail != null ? toEmail.trim().toLowerCase() : null;
    }
}
