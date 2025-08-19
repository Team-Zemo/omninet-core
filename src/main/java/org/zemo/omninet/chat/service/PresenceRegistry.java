package org.zemo.omninet.chat.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceRegistry {
    // email -> sessionId
    private final Map<String, String> online = new ConcurrentHashMap<>();

    @EventListener
    public void onConnect(SessionConnectEvent e) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(e.getMessage());
        String email = sha.getFirstNativeHeader("userEmail");
        if (email != null)
            online.put(email, sha.getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent e) {
        String sid = e.getSessionId();
        online.values().removeIf(v -> v.equals(sid));
    }

    public boolean isOnline(String email) {
        return online.containsKey(email);
    }
}
