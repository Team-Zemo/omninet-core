package org.zemo.omninet.chat.model;

import jakarta.persistence.*;
import lombok.*;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.chat.dto.CallOfferDTO;
import org.zemo.omninet.chat.dto.CallStatusDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
@Data @NoArgsConstructor @AllArgsConstructor
public class CallSession {

    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "callee_id", nullable = false)
    private User callee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallOfferDTO.CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatusDTO.CallState state = CallStatusDTO.CallState.INITIATING;

    @Column(nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    @Column(length = 1000)
    private String errorMessage;

    // WebRTC session data
    @Lob
    private String callerSdp;

    @Lob
    private String calleeSdp;
}
