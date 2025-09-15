package org.zemo.omninet.ai.controller;

import org.zemo.omninet.ai.model.ChatMessage;
import org.zemo.omninet.ai.model.ChatSession;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.zemo.omninet.notes.util.CommonUtil.getLoggedInUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createChatSession(
            @RequestBody Map<String, String> request) {

        String title = request.get("title");
        User user = getLoggedInUser();

        ChatSession session = chatService.createChatSession(user, title);

        Map<String, Object> response = new HashMap<>();
        response.put("id", session.getId());
        response.put("title", session.getTitle());
        response.put("createdAt", session.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getUserChatSessions(Authentication authentication) {
        User user = getLoggedInUser();
        List<ChatSession> sessions = chatService.getUserChatSessions(user);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSession> getChatSession(
            @PathVariable Long sessionId) {

        return chatService.getChatSessionById(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(
            @PathVariable Long sessionId) {

        return chatService.getChatSessionById(sessionId)
                .map(session -> {
                    List<ChatMessage> messages = chatService.getSessionMessages(session);
                    return ResponseEntity.ok(messages);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteChatSession(
            @PathVariable Long sessionId) {

        chatService.deleteChatSession(sessionId);
        return ResponseEntity.ok().build();
    }
}