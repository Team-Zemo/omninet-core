package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkReadRequest {

    @NotBlank(message = "My email cannot be blank")
    @Email(message = "Invalid my email format")
    private String myEmail;

    @NotBlank(message = "Other email cannot be blank")
    @Email(message = "Invalid other email format")
    private String otherEmail;

    // Sanitize emails
    public void setMyEmail(String myEmail) {
        this.myEmail = myEmail != null ? myEmail.trim().toLowerCase() : null;
    }

    public void setOtherEmail(String otherEmail) {
        this.otherEmail = otherEmail != null ? otherEmail.trim().toLowerCase() : null;
    }
}
