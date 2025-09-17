package org.zemo.omninet.ai.repository;


import org.zemo.omninet.ai.model.ChatMessage;
import org.zemo.omninet.ai.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
}