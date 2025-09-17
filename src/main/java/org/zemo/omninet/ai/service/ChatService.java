package org.zemo.omninet.ai.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninet.ai.model.ChatMessage;
import org.zemo.omninet.ai.model.ChatSession;
import org.zemo.omninet.ai.repository.ChatMessageRepository;
import org.zemo.omninet.ai.repository.ChatSessionRepository;
import org.zemo.omninet.security.model.User;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSession createChatSession(User user, String title) {
        ChatSession session = new ChatSession(user, title);
        return chatSessionRepository.save(session);
    }

    public List<ChatSession> getUserChatSessions(User user) {
        return chatSessionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<ChatSession> getChatSessionById(Long id) {
        return chatSessionRepository.findById(id);
    }

    @Transactional
    public ChatMessage saveMessage(ChatSession session, ChatMessage.MessageType type, String content) {
        ChatMessage message = new ChatMessage(session, type, content);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getSessionMessages(ChatSession session) {
        return chatMessageRepository.findByChatSessionOrderByCreatedAtAsc(session);
    }

    public void deleteChatSession(Long sessionId) {
        chatSessionRepository.deleteById(sessionId);
    }
}