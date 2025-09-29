package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallResponseDTO {

    @NotBlank(message = "Call ID cannot be blank")
    private String callId;

    @NotNull(message = "Response type cannot be null")
    private ResponseType responseType;

    private String sdpAnswer; // Only for ACCEPT
    private String reason; // Only for REJECT

    public enum ResponseType {
        ACCEPT, REJECT, BUSY
    }
}
