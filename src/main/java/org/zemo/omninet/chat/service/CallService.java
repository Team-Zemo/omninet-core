package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninet.chat.dto.*;
import org.zemo.omninet.chat.model.CallSession;
import org.zemo.omninet.chat.repository.CallSessionRepository;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.security.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final PresenceRegistry presenceRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<String, String> userActiveCallMap = new ConcurrentHashMap<>();

    @Transactional
    public CallStatusDTO initiateCall(String callerEmail, CallOfferDTO callOffer) {
        try {
            User caller = userRepository.findByEmail(callerEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Caller not found: " + callerEmail));

            User callee = userRepository.findByEmail(callOffer.getReceiverEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Callee not found: " + callOffer.getReceiverEmail()));

            if (!contactService.isContact(callerEmail, callOffer.getReceiverEmail())) {
                throw new IllegalStateException("Cannot call non-contact user");
            }

            if (isUserInActiveCall(callerEmail)) {
                throw new IllegalStateException("User is already in an active call");
            }

            if (isUserInActiveCall(callOffer.getReceiverEmail())) {
                throw new IllegalStateException("Recipient is already in an active call");
            }

            if (!presenceRegistry.isOnline(callOffer.getReceiverEmail())) {
                throw new IllegalStateException("Recipient is not online");
            }

            String callId = UUID.randomUUID().toString();

            CallSession session = new CallSession();
            session.setId(callId);
            session.setCaller(caller);
            session.setCallee(callee);
            session.setCallType(callOffer.getCallType());
            session.setState(CallStatusDTO.CallState.INITIATING);
            session.setCallerSdp(callOffer.getSdpOffer());

            callSessionRepository.save(session);

            userActiveCallMap.put(callerEmail, callId);
            userActiveCallMap.put(callOffer.getReceiverEmail(), callId);

            session.setState(CallStatusDTO.CallState.RINGING);
            callSessionRepository.save(session);

            CallStatusDTO callStatus = toStatusDTO(session);
            CallOfferDTO offerToSend = new CallOfferDTO(
                callOffer.getReceiverEmail(),
                callOffer.getCallType(),
                callOffer.getSdpOffer(),
                callId
            );
            messagingTemplate.convertAndSend("/queue/call-offer-" + callOffer.getReceiverEmail(), offerToSend);

            log.info("Call initiated: {} -> {}, type: {}, callId: {}",
                callerEmail, callOffer.getReceiverEmail(), callOffer.getCallType(), callId);

            return callStatus;

        } catch (Exception e) {
            log.error("Error initiating call from {} to {}: {}", callerEmail, callOffer.getReceiverEmail(), e.getMessage());
            throw new RuntimeException("Failed to initiate call: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CallStatusDTO respondToCall(String calleeEmail, CallResponseDTO response) {
        try {
            CallSession session = callSessionRepository.findById(response.getCallId())
                    .orElseThrow(() -> new IllegalArgumentException("Call session not found"));

            if (!session.getCallee().getEmail().equals(calleeEmail)) {
                throw new IllegalStateException("User not authorized to respond to this call");
            }

            if (session.getState() != CallStatusDTO.CallState.RINGING) {
                throw new IllegalStateException("Call is not in ringing state");
            }

            switch (response.getResponseType()) {
                case ACCEPT:
                    session.setState(CallStatusDTO.CallState.CONNECTING);
                    session.setCalleeSdp(response.getSdpAnswer());

                    messagingTemplate.convertAndSend("/queue/call-response-" + session.getCaller().getEmail(), response);
                    log.info("Call accepted: {}", response.getCallId());
                    break;

                case REJECT:
                case BUSY:
                    session.setState(CallStatusDTO.CallState.ENDED);
                    session.setEndTime(LocalDateTime.now());
                    session.setErrorMessage(response.getReason());

                    userActiveCallMap.remove(session.getCaller().getEmail());
                    userActiveCallMap.remove(session.getCallee().getEmail());

                    messagingTemplate.convertAndSend("/queue/call-response-" + session.getCaller().getEmail(), response);
                    log.info("Call rejected/busy: {}, reason: {}", response.getCallId(), response.getReason());
                    break;
            }

            callSessionRepository.save(session);
            return toStatusDTO(session);

        } catch (Exception e) {
            log.error("Error responding to call {}: {}", response.getCallId(), e.getMessage());
            throw new RuntimeException("Failed to respond to call: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void confirmConnection(String callId, String userEmail) {
        try {
            CallSession session = callSessionRepository.findById(callId)
                    .orElseThrow(() -> new IllegalArgumentException("Call session not found"));

            if (!session.getCaller().getEmail().equals(userEmail) &&
                !session.getCallee().getEmail().equals(userEmail)) {
                throw new IllegalStateException("User not part of this call");
            }

            if (session.getState() == CallStatusDTO.CallState.CONNECTING) {
                session.setState(CallStatusDTO.CallState.CONNECTED);
                callSessionRepository.save(session);

                CallStatusDTO status = toStatusDTO(session);
                messagingTemplate.convertAndSend("/queue/call-status-" + session.getCaller().getEmail(), status);
                messagingTemplate.convertAndSend("/queue/call-status-" + session.getCallee().getEmail(), status);

                log.info("Call connected: {}", callId);
            }
        } catch (Exception e) {
            log.error("Error confirming connection for call {}: {}", callId, e.getMessage());
            throw new RuntimeException("Failed to confirm connection: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void endCall(String callId, String userEmail, CallEndDTO.EndReason reason) {
        try {
            CallSession session = null;

            Optional<CallSession> sessionOpt = callSessionRepository.findById(callId);

            if (sessionOpt.isEmpty() && callId.startsWith("call-")) {
                String activeCallId = userActiveCallMap.get(userEmail);
                if (activeCallId != null) {
                    sessionOpt = callSessionRepository.findById(activeCallId);
                    log.info("Mapped frontend call ID {} to backend call ID {}", callId, activeCallId);
                }
            }

            if (sessionOpt.isEmpty()) {
                log.warn("Call session not found for callId: {}, cleaning up user active call tracking", callId);
                userActiveCallMap.remove(userEmail);
                return;
            }

            session = sessionOpt.get();

            if (!session.getCaller().getEmail().equals(userEmail) &&
                !session.getCallee().getEmail().equals(userEmail)) {
                throw new IllegalStateException("User not part of this call");
            }

            session.setState(CallStatusDTO.CallState.ENDED);
            session.setEndTime(LocalDateTime.now());
            if (reason != null) {
                session.setErrorMessage(reason.toString());
            }

            userActiveCallMap.remove(session.getCaller().getEmail());
            userActiveCallMap.remove(session.getCallee().getEmail());

            callSessionRepository.save(session);

            CallEndDTO endEvent = new CallEndDTO(session.getId(), reason);
            messagingTemplate.convertAndSend("/queue/call-end-" + session.getCaller().getEmail(), endEvent);
            messagingTemplate.convertAndSend("/queue/call-end-" + session.getCallee().getEmail(), endEvent);

            log.info("Call ended: {}, reason: {}, cleaned up active tracking for: {} and {}",
                session.getId(), reason, session.getCaller().getEmail(), session.getCallee().getEmail());

        } catch (Exception e) {
            log.error("Error ending call {}: {}", callId, e.getMessage());
            userActiveCallMap.remove(userEmail);
            throw new RuntimeException("Failed to end call: " + e.getMessage(), e);
        }
    }

    public void handleIceCandidate(String callId, String userEmail, IceCandidateDTO candidate) {
        try {
            CallSession session = null;
            Optional<CallSession> sessionOpt = callSessionRepository.findById(callId);
            if (sessionOpt.isEmpty() && callId.startsWith("call-")) {
                String activeCallId = userActiveCallMap.get(userEmail);
                if (activeCallId != null) {
                    sessionOpt = callSessionRepository.findById(activeCallId);
                    log.debug("Mapped frontend call ID {} to backend call ID {} for ICE candidate", callId, activeCallId);
                }
            }

            if (sessionOpt.isEmpty()) {
                log.warn("ICE candidate received for non-existent call session: {}", callId);
                return;
            }

            session = sessionOpt.get();

            if (!session.getCaller().getEmail().equals(userEmail) &&
                !session.getCallee().getEmail().equals(userEmail)) {
                throw new IllegalStateException("User not part of this call");
            }

            String otherPartyEmail = session.getCaller().getEmail().equals(userEmail) ?
                    session.getCallee().getEmail() : session.getCaller().getEmail();

            messagingTemplate.convertAndSend("/queue/ice-candidate-" + otherPartyEmail, candidate);

        } catch (Exception e) {
            log.error("Error handling ICE candidate for call {}: {}", callId, e.getMessage());
        }
    }

    public void forceCleanupUserCallState(String userEmail) {
        String removedCallId = userActiveCallMap.remove(userEmail);
        if (removedCallId != null) {
            log.info("Force cleaned up call state for user: {}, removed call ID: {}", userEmail, removedCallId);
        }
    }

    public boolean callSessionExists(String callId) {
        return callSessionRepository.existsById(callId);
    }

    public boolean isUserInActiveCall(String userEmail) {
        return userActiveCallMap.containsKey(userEmail);
    }

    public Optional<String> getUserActiveCallId(String userEmail) {
        return Optional.ofNullable(userActiveCallMap.get(userEmail));
    }

    public List<CallStatusDTO> getRecentCalls(String userEmail, int days) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<CallSession> sessions = callSessionRepository.findRecentCallsForUser(user, since);

        return sessions.stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupStaleCalls() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5); // 5 minutes timeout

        List<CallSession> staleCalls = callSessionRepository.findByStateAndStartTimeBefore(
                CallStatusDTO.CallState.RINGING, cutoff);

        for (CallSession session : staleCalls) {
            session.setState(CallStatusDTO.CallState.FAILED);
            session.setEndTime(LocalDateTime.now());
            session.setErrorMessage("Call timeout");

            userActiveCallMap.remove(session.getCaller().getEmail());
            userActiveCallMap.remove(session.getCallee().getEmail());

            CallEndDTO endEvent = new CallEndDTO(session.getId(), CallEndDTO.EndReason.TIMEOUT);
            messagingTemplate.convertAndSend("/queue/call-end-" + session.getCaller().getEmail(), endEvent);
            messagingTemplate.convertAndSend("/queue/call-end-" + session.getCallee().getEmail(), endEvent);
        }

        callSessionRepository.saveAll(staleCalls);
        log.info("Cleaned up {} stale calls", staleCalls.size());
    }

    private CallStatusDTO toStatusDTO(CallSession session) {
        return new CallStatusDTO(
                session.getId(),
                session.getCaller().getEmail(),
                session.getCallee().getEmail(),
                session.getCallType(),
                session.getState(),
                session.getStartTime(),
                session.getEndTime(),
                session.getErrorMessage()
        );
    }
}
