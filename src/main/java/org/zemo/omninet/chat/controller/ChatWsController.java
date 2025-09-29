package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.validation.annotation.Validated;
import org.zemo.omninet.chat.dto.*;
import org.zemo.omninet.chat.service.CallService;
import org.zemo.omninet.chat.service.MessageService;
import org.zemo.omninet.security.model.User;
import jakarta.validation.Valid;

import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class ChatWsController {

    private final MessageService messages;
    private final CallService callService;
    private final SimpMessagingTemplate broker;

    // Improved deduplication caches
    private final ConcurrentHashMap<String, Long> iceCandidateCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> callResponseCache = new ConcurrentHashMap<>();

    // Cache cleanup every 5 minutes
    private static final long CACHE_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private long lastCacheCleanup = System.currentTimeMillis();

    private void cleanupCaches() {
        long now = System.currentTimeMillis();
        if (now - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            // Remove entries older than 10 minutes
            long cutoff = now - 10 * 60 * 1000;
            iceCandidateCache.entrySet().removeIf(entry -> entry.getValue() < cutoff);
            callResponseCache.entrySet().removeIf(entry -> entry.getValue() < cutoff);
            lastCacheCleanup = now;
            log.debug("Cleaned up deduplication caches");
        }
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageDTO dto, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized message send attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            log.debug("User {} sending message to {}", currentUser.getEmail(), dto.getReceiverEmail());

            messages.send(currentUser.getEmail(), dto);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            // Send error back to sender
            broker.convertAndSendToUser(auth.getName(), "/queue/errors",
                "Failed to send message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.read")
    public void read(@Valid @Payload MarkReadRequest req, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized read request attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            messages.markRead(currentUser.getEmail(), req.getOtherEmail());
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Valid @Payload TypingEvent ev, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized typing event attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            ev.setFromEmail(currentUser.getEmail());
            broker.convertAndSend("/queue/typing-" + ev.getToEmail(), ev);
        } catch (Exception e) {
            log.error("Error handling typing event: {}", e.getMessage(), e);
        }
    }

    // Video/Voice Call WebSocket Handlers

    @MessageMapping("/call.offer")
    public void callOffer(@Valid @Payload CallOfferDTO offer, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized call offer attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            log.info("Call offer from {} to {} ({})", currentUser.getEmail(), offer.getReceiverEmail(), offer.getCallType());

            CallStatusDTO status = callService.initiateCall(currentUser.getEmail(), offer);
            broker.convertAndSendToUser(currentUser.getEmail(), "/queue/call-status", status);

        } catch (Exception e) {
            log.error("Error handling call offer: {}", e.getMessage(), e);
            broker.convertAndSendToUser(auth.getName(), "/queue/call-errors",
                "Failed to initiate call: " + e.getMessage());
        }
    }

    @MessageMapping("/call.response")
    public void callResponse(@Valid @Payload CallResponseDTO response, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized call response attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            log.info("Call response from {} for call {}: {}", currentUser.getEmail(), response.getCallId(), response.getResponseType());

            // Add deduplication for call responses
            String dedupeKey = response.getCallId() + "-" + response.getResponseType().name();
            if (callResponseCache.putIfAbsent(dedupeKey, System.currentTimeMillis()) != null) {
                log.debug("Ignoring duplicate call response for call {}", response.getCallId());
                return;
            }

            CallStatusDTO status = callService.respondToCall(currentUser.getEmail(), response);
            broker.convertAndSendToUser(currentUser.getEmail(), "/queue/call-status", status);

        } catch (Exception e) {
            log.error("Error handling call response: {}", e.getMessage(), e);
            broker.convertAndSendToUser(auth.getName(), "/queue/call-errors",
                "Failed to respond to call: " + e.getMessage());
        }
    }

    @MessageMapping("/call.end")
    public void endCall(@Valid @Payload CallEndDTO endCall, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized call end attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            log.info("Call end from {} for call {}: {}", currentUser.getEmail(), endCall.getCallId(), endCall.getReason());

            callService.endCall(endCall.getCallId(), currentUser.getEmail(), endCall.getReason());

        } catch (Exception e) {
            log.error("Error ending call: {}", e.getMessage(), e);
            broker.convertAndSendToUser(auth.getName(), "/queue/call-errors",
                "Failed to end call: " + e.getMessage());
        }
    }

    @MessageMapping("/call.ice-candidate")
    public void iceCandidate(@Valid @Payload IceCandidateDTO candidate, Authentication auth) {
        cleanupCaches(); // Clean up old cache entries

        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized ICE candidate attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();

            // Enhanced deduplication to prevent multiple processing of same ICE candidate
            String dedupeKey = candidate.getCallId() + "-" + candidate.getCandidate().substring(0, Math.min(50, candidate.getCandidate().length())).hashCode();

            if (iceCandidateCache.putIfAbsent(dedupeKey, System.currentTimeMillis()) != null) {
                log.debug("Ignoring duplicate ICE candidate for call {}", candidate.getCallId());
                return;
            }

            // Check if call session exists before processing
            if (!callService.callSessionExists(candidate.getCallId())) {
                log.warn("ICE candidate received for non-existent call session: {}", candidate.getCallId());
                return;
            }

            callService.handleIceCandidate(candidate.getCallId(), currentUser.getEmail(), candidate);
            log.debug("ICE candidate processed for call {}", candidate.getCallId());

        } catch (Exception e) {
            log.error("Error handling ICE candidate for call {}: {}", candidate.getCallId(), e.getMessage());
        }
    }

    @MessageMapping("/call.connected")
    public void callConnected(@Payload String callId, Authentication auth) {
        try {
            if (auth == null || !(auth.getPrincipal() instanceof User)) {
                log.warn("Unauthorized call connected attempt");
                return;
            }

            User currentUser = (User) auth.getPrincipal();
            callService.confirmConnection(callId, currentUser.getEmail());

        } catch (Exception e) {
            log.error("Error confirming call connection: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onConnect(SessionSubscribeEvent e) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(e.getMessage());
            Authentication authentication = (Authentication) accessor.getUser();

            if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
                String email = currentUser.getEmail();
                if (email != null) {
                    log.debug("User connected: {}", email);
                    messages.deliverPendingOnConnect(email);
                }
            }
        } catch (Exception ex) {
            log.error("Error handling connection event: {}", ex.getMessage(), ex);
        }
    }
}
