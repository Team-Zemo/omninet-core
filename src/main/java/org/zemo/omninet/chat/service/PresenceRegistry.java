package org.zemo.omninet.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.zemo.omninet.security.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PresenceRegistry {

    // Map: email -> session data
    private final Map<String, SessionData> userSessions = new ConcurrentHashMap<>();

    // Map: sessionId -> email for cleanup
    private final Map<String, String> sessionToEmail = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Inner class to track session data
    private static class SessionData {
        final String sessionId;
        final LocalDateTime connectedAt;
        LocalDateTime lastActivity;

        SessionData(String sessionId) {
            this.sessionId = sessionId;
            this.connectedAt = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }
    }

    public PresenceRegistry() {
        // Clean up stale sessions every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanupStaleSessions, 5, 5, TimeUnit.MINUTES);
    }

    @EventListener
    public void onConnect(SessionConnectEvent e) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(e.getMessage());
            Authentication auth = (Authentication) accessor.getUser();

            if (auth != null && auth.getPrincipal() instanceof User user) {
                String email = user.getEmail();
                String sessionId = accessor.getSessionId();

                if (email != null && sessionId != null) {
                    SessionData sessionData = new SessionData(sessionId);
                    userSessions.put(email, sessionData);
                    sessionToEmail.put(sessionId, email);

                    log.info("User connected: {} with session {}", email, sessionId);
                } else {
                    log.warn("Invalid connection data - email: {}, sessionId: {}", email, sessionId);
                }
            } else {
                log.warn("Connection attempt without valid authentication");
            }
        } catch (Exception ex) {
            log.error("Error handling connection event: {}", ex.getMessage(), ex);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent e) {
        try {
            String sessionId = e.getSessionId();
            if (sessionId != null) {
                String email = sessionToEmail.remove(sessionId);
                if (email != null) {
                    SessionData sessionData = userSessions.get(email);
                    if (sessionData != null && sessionData.sessionId.equals(sessionId)) {
                        userSessions.remove(email);
                        log.info("User disconnected: {} from session {}", email, sessionId);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error handling disconnect event: {}", ex.getMessage(), ex);
        }
    }

    public boolean isOnline(String email) {
        if (email == null) return false;

        SessionData sessionData = userSessions.get(email);
        if (sessionData == null) return false;

        // Check if session is not too old (5 minutes threshold)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return sessionData.lastActivity.isAfter(threshold);
    }

    public void updateUserActivity(String email) {
        if (email != null) {
            SessionData sessionData = userSessions.get(email);
            if (sessionData != null) {
                sessionData.updateActivity();
            }
        }
    }

    public Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }

    public LocalDateTime getLastActivity(String email) {
        SessionData sessionData = userSessions.get(email);
        return sessionData != null ? sessionData.lastActivity : null;
    }

    public LocalDateTime getConnectedAt(String email) {
        SessionData sessionData = userSessions.get(email);
        return sessionData != null ? sessionData.connectedAt : null;
    }

    private void cleanupStaleSessions() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

            userSessions.entrySet().removeIf(entry -> {
                SessionData sessionData = entry.getValue();
                if (sessionData.lastActivity.isBefore(threshold)) {
                    sessionToEmail.remove(sessionData.sessionId);
                    log.debug("Cleaned up stale session for user: {}", entry.getKey());
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            log.error("Error during session cleanup: {}", e.getMessage(), e);
        }
    }

    public int getOnlineUserCount() {
        return userSessions.size();
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
