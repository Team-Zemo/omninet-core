package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.zemo.omninet.chat.dto.MarkReadRequest;
import org.zemo.omninet.chat.dto.SendMessageDTO;
import org.zemo.omninet.chat.dto.TypingEvent;
import org.zemo.omninet.chat.service.MessageService;
import org.zemo.omninet.security.model.User;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messages;
    private final SimpMessagingTemplate broker;
    Authentication auth;

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageDTO dto, Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        messages.send(currentUser.getEmail(), dto);
    }

    @MessageMapping("/chat.read")
    public void read(@Payload MarkReadRequest req , Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        messages.markRead(currentUser.getEmail(), req.getOtherEmail());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent ev, Authentication auth) {
        User currentUser = (User) auth.getPrincipal();
        ev.setFromEmail(currentUser.getEmail());
        broker.convertAndSend("/queue/typing-" + ev.getToEmail(), ev);
    }

    @EventListener
    public void onConnect(SessionSubscribeEvent e) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(e.getMessage());
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication != null && authentication.getPrincipal() instanceof User currentUser) {
            String email = currentUser.getEmail();
            if (email != null) {
                messages.deliverPendingOnConnect(email);
            }
        }
    }
}
