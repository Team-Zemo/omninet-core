package org.zemo.omninet.chat.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallOfferDTO {

    @NotBlank(message = "Receiver email cannot be blank")
    @Email(message = "Invalid email format")
    private String receiverEmail;

    @NotNull(message = "Call type cannot be null")
    private CallType callType;

    @NotBlank(message = "SDP offer cannot be blank")
    private String sdpOffer;

    private String callId;

    public enum CallType {
        VOICE, VIDEO
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail != null ? receiverEmail.trim().toLowerCase() : null;
    }
}

