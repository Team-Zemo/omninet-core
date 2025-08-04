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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/user")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("User authenticated: {}", principal.getName());

        // Get user from database using provider ID - avoid API calls
        String providerId = getUserId(principal);
        Optional<User> userOpt = userService.getUserById(providerId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Update last login time only
            user.setLastLoginAt(LocalDateTime.now());
            user = userService.saveUser(user);
            return ResponseEntity.ok(user);
        } else {
            // New user - extract from provider
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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", principal != null);
        status.put("timestamp", LocalDateTime.now());

        if (principal != null) {
            String providerId = getUserId(principal);
            Optional<User> userOpt = userService.getUserById(providerId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastLoginAt(LocalDateTime.now());
                user = userService.saveUser(user);
                status.put("user", user);
            } else {
                User user = userService.saveOrUpdateUser(principal);
                status.put("user", user);
            }
        }

        return ResponseEntity.ok(status);
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
        profile.put("attributes", user.getAttributes()); // Use stored attributes
        profile.put("authorities", principal.getAuthorities());

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> debug = new HashMap<>();
        debug.put("principal_name", principal.getName());
        debug.put("live_attributes", principal.getAttributes()); // Current session attributes
        debug.put("email_attribute", principal.getAttribute("email"));
        debug.put("authorities", principal.getAuthorities());

        // Get stored user data from database
        String providerId = getUserId(principal);
        Optional<User> userOpt = userService.getUserById(providerId);

        if (userOpt.isPresent()) {
            User storedUser = userOpt.get();
            debug.put("stored_user", storedUser);
            debug.put("stored_attributes", storedUser.getAttributes());
        } else {
            User extractedUser = userService.extractUserFromPrincipal(principal);
            debug.put("extracted_user", extractedUser);
        }

        return ResponseEntity.ok(debug);
    }

    @GetMapping("/conflict-check")
    public ResponseEntity<Map<String, Object>> checkForConflicts(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> conflictInfo = userService.getAccountConflictInfo(principal);

        if (conflictInfo != null) {
            return ResponseEntity.ok(conflictInfo);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("hasConflict", false);
            response.put("message", "No account conflicts detected");
            return ResponseEntity.ok(response);
        }
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
            log.error("Error merging accounts: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to merge accounts: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/merge-info")
    public ResponseEntity<Map<String, Object>> getMergeInfo(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String providerId = getUserId(principal);
        Optional<User> userOpt = userService.getUserById(providerId);

        User currentUser;
        if (userOpt.isPresent()) {
            currentUser = userOpt.get();
        } else {
            currentUser = userService.saveOrUpdateUser(principal);
        }

        Map<String, Object> mergeInfo = new HashMap<>();
        mergeInfo.put("isAccountMerged", currentUser.isAccountMerged());
        mergeInfo.put("primaryProvider", currentUser.getPrimaryProvider());
        mergeInfo.put("linkedProviders", currentUser.getLinkedProviders());
        mergeInfo.put("totalLinkedProviders",
                currentUser.getLinkedProviders() != null ?
                        currentUser.getLinkedProviders().split(",").length : 1);

        return ResponseEntity.ok(mergeInfo);
    }

    private String getUserId(OAuth2User principal) {
        Object id = principal.getAttribute("id");
        if (id != null) {
            return id.toString();
        }

        // For Google, use 'sub' claim
        Object sub = principal.getAttribute("sub");
        if (sub != null) {
            return sub.toString();
        }

        return principal.getName();
    }
}
