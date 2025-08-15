package org.zemo.omninet.security.controller;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.security.repository.UserRepository;
import org.zemo.omninet.security.model.User;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/search")
    public Optional<User> searchByEmail(@RequestParam String email) {
        return userRepo.findByEmail(email);
    }
}
