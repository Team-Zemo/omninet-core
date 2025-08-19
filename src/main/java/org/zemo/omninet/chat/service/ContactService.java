package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninet.chat.model.Contact;
import org.zemo.omninet.chat.dto.ContactItem;
import org.zemo.omninet.chat.repository.ContactRepository;
import org.zemo.omninet.chat.repository.MessageRepository;
import org.zemo.omninet.security.repository.UserRepository;
import org.zemo.omninet.security.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contacts;
    private final UserRepository users;
    private final MessageRepository messages;
    private final PresenceRegistry presence;

    @Transactional
    public void addBidirectional(String meEmail, String contactEmail) {

        User me = users.findByEmail(meEmail).orElseThrow(() -> new IllegalArgumentException("User not found: "+meEmail));
        User other = users.findByEmail(contactEmail).orElseThrow(() -> new IllegalArgumentException("User not found: "+contactEmail));

        if (!contacts.existsByOwnerAndContact(me, other)) {
            Contact c = new Contact();
            c.setOwner(me);
            c.setContact(other);
            contacts.save(c);
        }

        if (!contacts.existsByOwnerAndContact(other, me)) {
            Contact c2 = new Contact();
            c2.setOwner(other);
            c2.setContact(me);
            contacts.save(c2);
        }
    }

    @Transactional
    public void ensureBidirectional(String meEmail, String contactEmail) {
        if (!isContact(meEmail, contactEmail)) {
            addBidirectional(meEmail, contactEmail);
        }
    }

    @Transactional
    public void touchLastMessage(String aEmail, String bEmail, LocalDateTime when) {
        User a = users.findByEmail(aEmail).get();
        User b = users.findByEmail(bEmail).get();

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
    }

    public boolean isContact(String meEmail, String otherEmail) {

        var me = users.findByEmail(meEmail).orElse(null);
        var other = users.findByEmail(otherEmail).orElse(null);

        return me != null && other != null && contacts.existsByOwnerAndContact(me, other);
    }

    public List<ContactItem> list(String meEmail) {

        User me = users.findByEmail(meEmail).orElseThrow(() -> new IllegalArgumentException("User not found: "+meEmail));
        var rows = contacts.findByOwner(me);

        List<ContactItem> out = new ArrayList<>();

        for (var r : rows) {
            var other = r.getContact();
            var last = messages.lastBetween(me.getId(), other.getId());
            String preview = last != null ? last.getContent() : null;
            var time = last != null ? last.getTimestamp() : null;
            long unread = messages.countUnreadFrom(me, other);
            boolean online = presence.isOnline(other.getEmail());
            out.add(new ContactItem(other.getEmail(), other.getName(), other.getAvatarUrl(), preview, time, unread, online));
        }

        return out;
    }
}