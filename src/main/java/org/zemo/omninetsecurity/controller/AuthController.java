package org.zemo.omninetsecurity.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.service.UserService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001","http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String providerId = getUserId(principal);
        Optional<User> userOpt = userService.getUserById(providerId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            user = userService.saveUser(user);
            return ResponseEntity.ok(user);
        } else {
            User user = userService.saveOrUpdateUser(principal);
            return ResponseEntity.ok(user);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String providerId = getUserId(principal);
        Optional<User> userOpt = userService.getUserById(providerId);

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            user = userService.saveUser(user);
        } else {
            user = userService.saveOrUpdateUser(principal);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("user", user);
        profile.put("attributes", user.getAttributes());
        profile.put("authorities", principal.getAuthorities());

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/merge-accounts")
    public ResponseEntity<Map<String, Object>> mergeAccounts(@AuthenticationPrincipal OAuth2User principal,
                                                             @RequestParam(defaultValue = "true") boolean confirm) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            User mergedUser = userService.saveOrUpdateUser(principal, confirm);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Accounts merged successfully");
            response.put("user", mergedUser);
            response.put("mergedProviders", mergedUser.getLinkedProviders());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to merge accounts: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    private String getUserId(OAuth2User principal) {
        Object id = principal.getAttribute("id");
        if (id != null) {
            return id.toString();
        }

        Object sub = principal.getAttribute("sub");
        if (sub != null) {
            return sub.toString();
        }

        return principal.getName();
    }
}
