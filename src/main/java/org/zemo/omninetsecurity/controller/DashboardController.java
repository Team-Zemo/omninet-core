package org.zemo.omninetsecurity.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.exception.AccountConflictException;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String providerId = getUserId(principal);
            Optional<User> userOpt = userService.getUserById(providerId);

            User currentUser;
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                currentUser.setLastLoginAt(LocalDateTime.now());
                currentUser = userService.saveUser(currentUser);
            } else {
                currentUser = userService.saveOrUpdateUser(principal);
            }

            Map<String, Object> userStats = userService.getUserStats();

            model.addAttribute("user", currentUser);
            model.addAttribute("attributes", currentUser.getAttributes());
            model.addAttribute("stats", userStats);

            return "dashboard";

        } catch (AccountConflictException e) {
            return "redirect:/merge-confirmation";
        } catch (Exception e) {
            log.error("Error in dashboard: {}", e.getMessage(), e);
            return "redirect:/login?error=true";
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

    @GetMapping("/api/dashboard/users")
    @ResponseBody
    public ResponseEntity<List<User>> getAllUsers(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}
