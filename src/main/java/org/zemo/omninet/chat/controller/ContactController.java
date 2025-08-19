package org.zemo.omninet.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.chat.dto.AddContactRequest;
import org.zemo.omninet.chat.dto.ContactItem;
import org.zemo.omninet.chat.service.ContactService;
import static org.zemo.omninet.notes.util.CommonUtil.getLoggedInUser;
import org.zemo.omninet.security.model.User;

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

    @GetMapping("/list")
    public List<ContactItem> list() {
        User user = getLoggedInUser();
        if (user == null)
            return null;
        return contactService.list(user.getEmail());
    }
}
