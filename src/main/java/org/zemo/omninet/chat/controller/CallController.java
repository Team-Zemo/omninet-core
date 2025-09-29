package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.chat.dto.CallStatusDTO;
import org.zemo.omninet.chat.service.CallService;
import org.zemo.omninet.security.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat/calls")
@RequiredArgsConstructor
@Slf4j
public class CallController {

    private final CallService callService;

    @GetMapping("/active")
    public ResponseEntity<Optional<String>> getActiveCall(Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            Optional<String> activeCallId = callService.getUserActiveCallId(currentUser.getEmail());
            return ResponseEntity.ok(activeCallId);
        } catch (Exception e) {
            log.error("Error getting active call for user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/status/{callId}")
    public ResponseEntity<String> getCallStatus(@PathVariable String callId, Authentication auth) {
        try {
            User currentUser = (User) auth.getPrincipal();
            boolean isInCall = callService.getUserActiveCallId(currentUser.getEmail())
                    .map(id -> id.equals(callId))
                    .orElse(false);

            if (!isInCall) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to view this call");
            }

            return ResponseEntity.ok("Call is active");
        } catch (Exception e) {
            log.error("Error getting call status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error retrieving call status");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<CallStatusDTO>> getCallHistory(
            Authentication auth,
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int days) {
        try {
            User currentUser = (User) auth.getPrincipal();
            List<CallStatusDTO> history = callService.getRecentCalls(currentUser.getEmail(), days);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting call history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupStaleCalls(Authentication auth) {
        try {
            callService.cleanupStaleCalls();
            return ResponseEntity.ok("Stale calls cleaned up successfully");
        } catch (Exception e) {
            log.error("Error cleaning up stale calls: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to cleanup stale calls");
        }
    }
}
