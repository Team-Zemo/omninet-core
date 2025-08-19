package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.chat.dto.HistoryPage;
import org.zemo.omninet.chat.dto.MarkReadRequest;
import org.zemo.omninet.chat.service.MessageService;
import org.zemo.omninet.security.model.User;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/history")
    public HistoryPage history(@RequestParam String otherEmail,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size, Authentication auth) {

        User user = (User) auth.getPrincipal();
        return messageService.history(user.getEmail(), otherEmail, page, size);
    }

    @PostMapping("/mark-read")
    public void markRead(@RequestBody MarkReadRequest req, Authentication auth) {
        User user = (User) auth.getPrincipal();
        messageService.markRead(user.getEmail(), req.getOtherEmail());
    }
}
