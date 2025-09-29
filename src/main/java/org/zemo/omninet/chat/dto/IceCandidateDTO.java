package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IceCandidateDTO {

    @NotBlank(message = "Call ID cannot be blank")
    private String callId;

    @NotBlank(message = "Candidate cannot be blank")
    private String candidate;

    @NotBlank(message = "SDP mid cannot be blank")
    private String sdpMid;

    private int sdpMLineIndex;
}
