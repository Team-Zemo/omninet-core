package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.zemo.omninet.chat.dto.MarkReadRequest;
import org.zemo.omninet.chat.dto.SendMessageDTO;
import org.zemo.omninet.chat.dto.TypingEvent;
import org.zemo.omninet.chat.service.MessageService;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messages;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageDTO dto) {
        messages.send(dto);
    }

    @MessageMapping("/chat.read")
    public void read(@Payload MarkReadRequest req) {
        messages.markRead(req.getMeEmail(), req.getOtherEmail());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent ev) {
        broker.convertAndSend("/queue/typing-" + ev.getToEmail(), ev);
    }

    @EventListener
    public void onConnect(SessionSubscribeEvent e) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(e.getMessage());
        String email = sha.getFirstNativeHeader("userEmail");
        if (email != null) messages.deliverPendingOnConnect(email);
    }
}
