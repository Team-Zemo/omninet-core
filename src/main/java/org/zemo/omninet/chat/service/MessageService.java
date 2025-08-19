package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messages;
    private final UserRepository users;
    private final ContactService contacts;
    private final PresenceRegistry presence;
    private final SimpMessagingTemplate broker;
    private final MessageQueueService mq;

    @Transactional
    public MessageView send(String senderEmail, SendMessageDTO dto) {

        User sender = users.findByEmail(senderEmail).orElseThrow(() -> new IllegalArgumentException("User not found: " + senderEmail));
        User receiver = users.findByEmail(dto.getReceiverEmail()).orElseThrow(() -> new IllegalArgumentException("User not found: " + dto.getReceiverEmail()));

        contacts.ensureBidirectional(sender.getEmail(), receiver.getEmail());

        Message m = new Message();
        m.setSender(sender);
        m.setReceiver(receiver);
        m.setContent(dto.getContent());
        m.setTimestamp(LocalDateTime.now());

        boolean receiverOnline = presence.isOnline(receiver.getEmail());
        m.setStatus(receiverOnline ? Message.Status.DELIVERED : Message.Status.PENDING);

        messages.save(m);
        contacts.touchLastMessage(sender.getEmail(), receiver.getEmail(), m.getTimestamp());

        MessageView view = toView(m);

        if (receiverOnline) {
            broker.convertAndSend("/queue/messages-" + receiver.getEmail(), view);
        } else {
            mq.publishUndelivered(view);
        }

        broker.convertAndSend("/queue/messages-" + sender.getEmail(), view);
        return view;
    }

    @Transactional
    public void deliverPendingOnConnect(String email) {

        List<MessageView> queued = mq.drainFor(email);
        if (!queued.isEmpty()) {
            for (MessageView v : queued) {
                broker.convertAndSend("/queue/messages-" + email, v);
                messages.findById(v.getId()).ifPresent(m -> {
                    m.setStatus(Message.Status.DELIVERED);
                    messages.save(m);
                });
            }
        }

        User u = users.findByEmail(email).orElse(null);

        if (u == null)
            return;

        List<Message> pending = messages.findByReceiverAndStatus(u, Message.Status.PENDING);
        if (!pending.isEmpty()) {
            for (Message m : pending) {
                m.setStatus(Message.Status.DELIVERED);
                broker.convertAndSend("/queue/messages-" + email, toView(m));
            }
            messages.saveAll(pending);
        }
    }

    public HistoryPage history(String meEmail, String otherEmail, int page, int size) {
        User me = users.findByEmail(meEmail).orElseThrow();
        User other = users.findByEmail(otherEmail).orElseThrow();
        Page<Message> p = messages.conversation(me, other, PageRequest.of(page, size));
        var items = p.getContent().stream().map(this::toView).collect(Collectors.toList());
        return new HistoryPage(items, page, size, p.hasNext());
    }

    @Transactional
    public void markRead(String meEmail, String otherEmail) {

        User me = users.findByEmail(meEmail).orElseThrow();
        User other = users.findByEmail(otherEmail).orElseThrow();

        var unread = messages.findUnreadFrom(me, other);

        for (var m : unread)
            m.setStatus(Message.Status.READ);

        messages.saveAll(unread);
        broker.convertAndSend("/queue/read-" + other.getEmail(), new MarkReadRequest(meEmail, otherEmail));
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
