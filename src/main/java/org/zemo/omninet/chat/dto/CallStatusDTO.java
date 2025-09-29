package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallStatusDTO {

    private String callId;
    private String callerEmail;
    private String calleeEmail;
    private CallOfferDTO.CallType callType;
    private CallState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;

    public enum CallState {
        INITIATING, RINGING, CONNECTING, CONNECTED, ENDED, FAILED
    }
}
