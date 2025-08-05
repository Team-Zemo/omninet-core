package org.zemo.omninetsecurity.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.security.dto.ApiResponse;
import org.zemo.omninetsecurity.security.dto.EmailLoginRequest;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.service.AuthenticationService;
import org.zemo.omninetsecurity.security.service.UserService;

import jakarta.validation.Valid;
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
    private final AuthenticationService authenticationService;

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
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
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithEmail(@Valid @RequestBody EmailLoginRequest request) {
        try {
            ApiResponse<Map<String, Object>> response = authenticationService.authenticateUser(
                request.getEmail(),
                request.getPassword()
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error during email login: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Login failed", e.getMessage())
            );
        }
    }

    @GetMapping("/check-methods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuthenticationMethods(@RequestParam String email) {
        try {
            ApiResponse<Map<String, Object>> response = authenticationService.checkAuthenticationMethods(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking authentication methods: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Error checking authentication methods")
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
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

    @PostMapping("/add-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addPasswordToAccount(
            @RequestParam String email,
            @RequestParam String password) {
        try {
            ApiResponse<Map<String, Object>> response = authenticationService.addPasswordToOAuthAccount(email, password);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error adding password to account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to add password authentication")
            );
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
