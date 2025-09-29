package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;
import org.zemo.omninet.chat.model.Message;
import org.zemo.omninet.chat.dto.HistoryPage;
import org.zemo.omninet.chat.dto.MarkReadRequest;
import org.zemo.omninet.chat.dto.MessageView;
import org.zemo.omninet.chat.dto.SendMessageDTO;
import org.zemo.omninet.chat.repository.MessageRepository;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.security.repository.UserRepository;
import org.zemo.omninet.chat.mq.MessageQueueService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messages;
    private final UserRepository users;
    private final ContactService contacts;
    private final PresenceRegistry presence;
    private final SimpMessagingTemplate broker;
    private final MessageQueueService mq;

    // Cache for user lookups to reduce redundant database queries
    private final ConcurrentHashMap<String, User> userCache = new ConcurrentHashMap<>();

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MessageView send(String senderEmail, SendMessageDTO dto) {
        try {
            // Input validation
            if (!StringUtils.hasText(senderEmail)) {
                throw new IllegalArgumentException("Sender email cannot be empty");
            }
            if (!StringUtils.hasText(dto.getReceiverEmail())) {
                throw new IllegalArgumentException("Receiver email cannot be empty");
            }
            if (!StringUtils.hasText(dto.getContent())) {
                throw new IllegalArgumentException("Message content cannot be empty");
            }
            if (senderEmail.equals(dto.getReceiverEmail())) {
                throw new IllegalArgumentException("Cannot send message to yourself");
            }

            User sender = getUserFromCacheOrDb(senderEmail);
            User receiver = getUserFromCacheOrDb(dto.getReceiverEmail());

            // Ensure users are contacts (this also validates user existence)
            contacts.ensureBidirectional(sender.getEmail(), receiver.getEmail());

            Message m = new Message();
            m.setSender(sender);
            m.setReceiver(receiver);
            m.setContent(dto.getContent()); // Content is already sanitized in DTO
            m.setTimestamp(LocalDateTime.now());

            boolean receiverOnline = presence.isOnline(receiver.getEmail());
            m.setStatus(receiverOnline ? Message.Status.DELIVERED : Message.Status.PENDING);

            messages.save(m);

            // Update contact last message timestamp
            contacts.touchLastMessage(sender.getEmail(), receiver.getEmail(), m.getTimestamp());

            MessageView view = toView(m);

            // Deliver message based on receiver's online status
            if (receiverOnline) {
                broker.convertAndSend("/queue/messages-" + receiver.getEmail(), view);
                log.debug("Message delivered immediately to online user: {}", receiver.getEmail());
            } else {
                mq.publishUndelivered(view);
                log.debug("Message queued for offline user: {}", receiver.getEmail());
            }

            // Always send confirmation to sender
            broker.convertAndSend("/queue/messages-" + sender.getEmail(), view);

            log.info("Message sent from {} to {}, status: {}", senderEmail, dto.getReceiverEmail(), m.getStatus());
            return view;

        } catch (Exception e) {
            log.error("Error sending message from {} to {}: {}", senderEmail, dto.getReceiverEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deliverPendingOnConnect(String email) {
        try {
            if (!StringUtils.hasText(email)) {
                log.warn("Empty email provided for pending message delivery");
                return;
            }

            // First, drain messages from queue
            List<MessageView> queued = mq.drainFor(email);
            if (!queued.isEmpty()) {
                for (MessageView v : queued) {
                    broker.convertAndSend("/queue/messages-" + email, v);
                    // Update message status to DELIVERED
                    messages.findById(v.getId()).ifPresent(m -> {
                        if (m.getStatus() == Message.Status.PENDING) {
                            m.setStatus(Message.Status.DELIVERED);
                            messages.save(m);
                        }
                    });
                }
                log.info("Delivered {} queued messages to {}", queued.size(), email);
            }

            // Then, handle any remaining PENDING messages in database
            Optional<User> userOpt = users.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("User not found for pending message delivery: {}", email);
                return;
            }

            User user = userOpt.get();
            List<Message> pending = messages.findByReceiverAndStatus(user, Message.Status.PENDING);
            if (!pending.isEmpty()) {
                for (Message m : pending) {
                    m.setStatus(Message.Status.DELIVERED);
                    MessageView view = toView(m);
                    broker.convertAndSend("/queue/messages-" + email, view);
                }
                messages.saveAll(pending);
                log.info("Delivered {} pending database messages to {}", pending.size(), email);
            }

        } catch (Exception e) {
            log.error("Error delivering pending messages to {}: {}", email, e.getMessage(), e);
        }
    }

    public HistoryPage history(String meEmail, String otherEmail, int page, int size) {
        try {
            // Input validation
            if (!StringUtils.hasText(meEmail) || !StringUtils.hasText(otherEmail)) {
                throw new IllegalArgumentException("Email addresses cannot be empty");
            }
            if (page < 0 || size <= 0 || size > 100) {
                throw new IllegalArgumentException("Invalid pagination parameters");
            }

            User me = getUserFromCacheOrDb(meEmail);
            User other = getUserFromCacheOrDb(otherEmail);

            // Check if users are contacts
            if (!contacts.isContact(meEmail, otherEmail)) {
                throw new IllegalStateException("Cannot view history with non-contact user");
            }

            Page<Message> p = messages.conversation(me, other, PageRequest.of(page, size));
            List<MessageView> items = p.getContent().stream()
                    .map(this::toView)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} messages for conversation between {} and {}", items.size(), meEmail, otherEmail);
            return new HistoryPage(items, page, size, p.hasNext());

        } catch (Exception e) {
            log.error("Error retrieving history for {} and {}: {}", meEmail, otherEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve message history: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void markRead(String meEmail, String otherEmail) {
        try {
            // Input validation
            if (!StringUtils.hasText(meEmail) || !StringUtils.hasText(otherEmail)) {
                throw new IllegalArgumentException("Email addresses cannot be empty");
            }

            User me = getUserFromCacheOrDb(meEmail);
            User other = getUserFromCacheOrDb(otherEmail);

            List<Message> unread = messages.findUnreadFrom(me, other);
            if (!unread.isEmpty()) {
                for (Message m : unread) {
                    m.setStatus(Message.Status.READ);
                }
                messages.saveAll(unread);

                // Notify the sender that their messages were read
                broker.convertAndSend("/queue/read-" + other.getEmail(),
                    new MarkReadRequest(meEmail, otherEmail));

                log.info("Marked {} messages as read for {} from {}", unread.size(), meEmail, otherEmail);
            }

        } catch (Exception e) {
            log.error("Error marking messages as read for {} from {}: {}", meEmail, otherEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to mark messages as read: " + e.getMessage(), e);
        }
    }

    // Get unread message count for a user from another user
    public long getUnreadCount(String meEmail, String otherEmail) {
        try {
            User me = getUserFromCacheOrDb(meEmail);
            User other = getUserFromCacheOrDb(otherEmail);

            return messages.countUnreadFrom(me, other);
        } catch (Exception e) {
            log.error("Error getting unread count for {} from {}: {}", meEmail, otherEmail, e.getMessage(), e);
            return 0;
        }
    }

    // Helper method to get user from cache or database to reduce redundant queries
    private User getUserFromCacheOrDb(String email) {
        return userCache.computeIfAbsent(email, e ->
            users.findByEmail(e).orElseThrow(() ->
                new IllegalArgumentException("User not found: " + e)));
    }

    // Clear user cache periodically to avoid stale data
    public void clearUserCache() {
        userCache.clear();
        log.debug("User cache cleared");
    }

    private MessageView toView(Message m) {
        return new MessageView(
                m.getId(),
                m.getSender().getEmail(),
                m.getReceiver().getEmail(),
                m.getContent(),
                m.getTimestamp(),
                m.getStatus().name()
        );
    }
}
