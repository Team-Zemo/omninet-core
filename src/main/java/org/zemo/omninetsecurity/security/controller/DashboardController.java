package org.zemo.omninetsecurity.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.service.UserService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserService userService;


    @GetMapping("/api/dashboard/users")
    @ResponseBody
    public ResponseEntity<List<User>> getAllUsers(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/confirm-merge")
    public String confirmMerge(HttpServletRequest request) {
        HttpSession session = request.getSession();

        OAuth2User pendingPrincipal = (OAuth2User) session.getAttribute("pendingPrincipal");
        Authentication pendingAuthentication = (Authentication) session.getAttribute("pendingAuthentication");

        if (pendingPrincipal == null || pendingAuthentication == null) {
            return "redirect:http://localhost:5173/login?error=true";
        }

        try {
            User mergedUser = userService.saveOrUpdateUser(pendingPrincipal, true);

            SecurityContextHolder.getContext().setAuthentication(pendingAuthentication);

            session.removeAttribute("conflictEmail");
            session.removeAttribute("existingProvider");
            session.removeAttribute("newProvider");
            session.removeAttribute("pendingPrincipal");
            session.removeAttribute("pendingAuthentication");

            return "redirect:http://localhost:5173/dashboard";

        } catch (Exception e) {
            log.error("Error during merge confirmation: {}", e.getMessage(), e);
            return "redirect:http://localhost:5173/merge-confirmation?error=true";
        }
    }
}
