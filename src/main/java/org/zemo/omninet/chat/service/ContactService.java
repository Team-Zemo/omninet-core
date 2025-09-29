package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.StringUtils;
import org.zemo.omninet.chat.model.Contact;
import org.zemo.omninet.chat.dto.ContactItem;
import org.zemo.omninet.chat.repository.ContactRepository;
import org.zemo.omninet.chat.repository.MessageRepository;
import org.zemo.omninet.security.repository.UserRepository;
import org.zemo.omninet.security.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contacts;
    private final UserRepository users;
    private final MessageRepository messages;
    private final PresenceRegistry presence;

    // Cache for contact relationships to reduce database queries
    private final ConcurrentHashMap<String, Boolean> contactCache = new ConcurrentHashMap<>();

    private String getCacheKey(String email1, String email2) {
        return email1.compareTo(email2) < 0 ? email1 + ":" + email2 : email2 + ":" + email1;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addBidirectional(String meEmail, String contactEmail) {
        try {
            // Input validation
            if (!StringUtils.hasText(meEmail) || !StringUtils.hasText(contactEmail)) {
                throw new IllegalArgumentException("Email addresses cannot be empty");
            }
            if (meEmail.equals(contactEmail)) {
                throw new IllegalArgumentException("Cannot add yourself as contact");
            }

            User me = users.findByEmail(meEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + meEmail));
            User other = users.findByEmail(contactEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + contactEmail));

            // Check if contact relationship already exists to avoid redundant operations
            boolean meToOtherExists = contacts.existsByOwnerAndContact(me, other);
            boolean otherToMeExists = contacts.existsByOwnerAndContact(other, me);

            if (!meToOtherExists) {
                Contact c = new Contact();
                c.setOwner(me);
                c.setContact(other);
                contacts.save(c);
                log.debug("Added contact relationship: {} -> {}", meEmail, contactEmail);
            }

            if (!otherToMeExists) {
                Contact c2 = new Contact();
                c2.setOwner(other);
                c2.setContact(me);
                contacts.save(c2);
                log.debug("Added contact relationship: {} -> {}", contactEmail, meEmail);
            }

            // Update cache
            String cacheKey = getCacheKey(meEmail, contactEmail);
            contactCache.put(cacheKey, true);

            log.info("Bidirectional contact added: {} <-> {}", meEmail, contactEmail);

        } catch (Exception e) {
            log.error("Error adding bidirectional contact {} <-> {}: {}", meEmail, contactEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to add contact: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void ensureBidirectional(String meEmail, String contactEmail) {
        try {
            if (!isContact(meEmail, contactEmail)) {
                addBidirectional(meEmail, contactEmail);
            }
        } catch (Exception e) {
            log.error("Error ensuring bidirectional contact {} <-> {}: {}", meEmail, contactEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to ensure contact relationship: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void touchLastMessage(String aEmail, String bEmail, LocalDateTime when) {
        try {
            if (!StringUtils.hasText(aEmail) || !StringUtils.hasText(bEmail) || when == null) {
                log.warn("Invalid parameters for touchLastMessage: {}, {}, {}", aEmail, bEmail, when);
                return;
            }

            Optional<User> aOpt = users.findByEmail(aEmail);
            Optional<User> bOpt = users.findByEmail(bEmail);

            if (aOpt.isEmpty() || bOpt.isEmpty()) {
                log.warn("Users not found for touchLastMessage: {} or {}", aEmail, bEmail);
                return;
            }

            User a = aOpt.get();
            User b = bOpt.get();

            // Update both directions efficiently
            Contact ab = contacts.findByOwnerAndContact(a, b);
            if (ab != null) {
                ab.setLastMessageAt(when);
                contacts.save(ab);
            }

            Contact ba = contacts.findByOwnerAndContact(b, a);
            if (ba != null) {
                ba.setLastMessageAt(when);
                contacts.save(ba);
            }

        } catch (Exception e) {
            log.error("Error touching last message for {} and {}: {}", aEmail, bEmail, e.getMessage(), e);
        }
    }

    public boolean isContact(String meEmail, String otherEmail) {
        try {
            if (!StringUtils.hasText(meEmail) || !StringUtils.hasText(otherEmail)) {
                return false;
            }
            if (meEmail.equals(otherEmail)) {
                return false; // Users are not contacts with themselves
            }

            String cacheKey = getCacheKey(meEmail, otherEmail);

            // Check cache first
            Boolean cached = contactCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // Check database
            Optional<User> meOpt = users.findByEmail(meEmail);
            Optional<User> otherOpt = users.findByEmail(otherEmail);

            if (meOpt.isEmpty() || otherOpt.isEmpty()) {
                contactCache.put(cacheKey, false);
                return false;
            }

            boolean isContact = contacts.existsByOwnerAndContact(meOpt.get(), otherOpt.get());

            // Cache the result
            contactCache.put(cacheKey, isContact);

            return isContact;

        } catch (Exception e) {
            log.error("Error checking contact relationship {} <-> {}: {}", meEmail, otherEmail, e.getMessage(), e);
            return false;
        }
    }

    public List<ContactItem> list(String meEmail) {
        try {
            if (!StringUtils.hasText(meEmail)) {
                throw new IllegalArgumentException("User email cannot be empty");
            }

            User me = users.findByEmail(meEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + meEmail));

            List<Contact> contactRows = contacts.findByOwner(me);
            List<ContactItem> result = new ArrayList<>();

            for (Contact contactRow : contactRows) {
                User other = contactRow.getContact();

                // Get last message efficiently
                var lastMessage = messages.lastBetween(me.getId(), other.getId());
                String preview = lastMessage != null ?
                    (lastMessage.getContent().length() > 50 ?
                        lastMessage.getContent().substring(0, 50) + "..." :
                        lastMessage.getContent()) : null;

                LocalDateTime lastMessageTime = lastMessage != null ? lastMessage.getTimestamp() : null;

                // Get unread count
                long unreadCount = messages.countUnreadFrom(me, other);

                // Check online status
                boolean isOnline = presence.isOnline(other.getEmail());

                ContactItem item = new ContactItem(
                    other.getEmail(),
                    other.getName(),
                    other.getAvatarUrl(),
                    preview,
                    lastMessageTime,
                    unreadCount,
                    isOnline
                );

                result.add(item);
            }

            // Sort by last message time (most recent first)
            result.sort((a, b) -> {
                if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                if (a.getLastMessageTime() == null) return 1;
                if (b.getLastMessageTime() == null) return -1;
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            });

            log.debug("Retrieved {} contacts for user {}", result.size(), meEmail);
            return result;

        } catch (Exception e) {
            log.error("Error listing contacts for {}: {}", meEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve contacts: " + e.getMessage(), e);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeContact(String meEmail, String contactEmail) {
        try {
            if (!StringUtils.hasText(meEmail) || !StringUtils.hasText(contactEmail)) {
                throw new IllegalArgumentException("Email addresses cannot be empty");
            }

            User me = users.findByEmail(meEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + meEmail));
            User other = users.findByEmail(contactEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactEmail));

            // Remove both directions
            Contact meToOther = contacts.findByOwnerAndContact(me, other);
            if (meToOther != null) {
                contacts.delete(meToOther);
            }

            Contact otherToMe = contacts.findByOwnerAndContact(other, me);
            if (otherToMe != null) {
                contacts.delete(otherToMe);
            }

            // Update cache
            String cacheKey = getCacheKey(meEmail, contactEmail);
            contactCache.put(cacheKey, false);

            log.info("Contact removed: {} <-> {}", meEmail, contactEmail);

        } catch (Exception e) {
            log.error("Error removing contact {} <-> {}: {}", meEmail, contactEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to remove contact: " + e.getMessage(), e);
        }
    }

    public void clearContactCache() {
        contactCache.clear();
        log.debug("Contact cache cleared");
    }
}