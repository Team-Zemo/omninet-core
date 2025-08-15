package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.chat.dto.HistoryPage;
import org.zemo.omninet.chat.dto.MarkReadRequest;
import org.zemo.omninet.chat.service.MessageService;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/history")
    public HistoryPage history(@RequestParam String meEmail, @RequestParam String otherEmail,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return messageService.history(meEmail, otherEmail, page, size);
    }

    @PostMapping("/mark-read")
    public void markRead(@RequestBody MarkReadRequest req) {
        messageService.markRead(req.getMeEmail(), req.getOtherEmail());
    }
}
