package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallEndDTO {

    @NotBlank(message = "Call ID cannot be blank")
    private String callId;

    private EndReason reason;

    public enum EndReason {
        USER_HANGUP, USER_ENDED, CALL_REJECTED, CONNECTION_LOST, ERROR, TIMEOUT
    }
}
