package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.chat.dto.AddContactRequest;
import org.zemo.omninet.chat.dto.ContactItem;
import org.zemo.omninet.chat.service.ContactService;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/add")
    public void add(@RequestBody AddContactRequest req) {
        contactService.addBidirectional(req.getMeEmail(), req.getContactEmail());
    }

    @GetMapping("/list/{meEmail}")
    public List<ContactItem> list(@PathVariable String meEmail) {
        return contactService.list(meEmail);
    }
}
