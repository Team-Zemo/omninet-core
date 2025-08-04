package org.zemo.omninetsecurity.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.HashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("name"));
            model.addAttribute("login", principal.getAttribute("login"));
            model.addAttribute("email", principal.getAttribute("email"));
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/merge-confirmation")
    public String mergeConfirmation(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();

        // Get conflict information from session
        String conflictEmail = (String) session.getAttribute("conflictEmail");
        String existingProvider = (String) session.getAttribute("existingProvider");
        String newProvider = (String) session.getAttribute("newProvider");
        OAuth2User pendingPrincipal = (OAuth2User) session.getAttribute("pendingPrincipal");

        if (conflictEmail == null || existingProvider == null || newProvider == null || pendingPrincipal == null) {
            // No conflict data in session, redirect to login
            return "redirect:/login";
        }

        // Get detailed conflict information
        Map<String, Object> conflictInfo = userService.getAccountConflictInfo(pendingPrincipal);

        if (conflictInfo != null) {
            model.addAllAttributes(conflictInfo);
        } else {
            // Fallback - create basic conflict info from session data
            model.addAttribute("email", conflictEmail);
            model.addAttribute("conflictType", "DIFFERENT_PROVIDER");

            Map<String, Object> existingAccount = new HashMap<>();
            existingAccount.put("provider", existingProvider);
            model.addAttribute("existingAccount", existingAccount);

            Map<String, Object> newAccount = new HashMap<>();
            newAccount.put("provider", newProvider);
            newAccount.put("name", pendingPrincipal.getAttribute("name"));
            model.addAttribute("newAccount", newAccount);
        }

        return "merge-confirmation";
    }

    @PostMapping("/confirm-merge")
    public String confirmMerge(HttpServletRequest request) {
        HttpSession session = request.getSession();

        // Get the pending principal and authentication from session
        OAuth2User pendingPrincipal = (OAuth2User) session.getAttribute("pendingPrincipal");
        Authentication pendingAuthentication = (Authentication) session.getAttribute("pendingAuthentication");

        if (pendingPrincipal == null || pendingAuthentication == null) {
            return "redirect:/login?error=true";
        }

        try {
            // Merge accounts with confirmation
            User mergedUser = userService.saveOrUpdateUser(pendingPrincipal, true);

            // Set the authentication in the security context so user is logged in
            SecurityContextHolder.getContext().setAuthentication(pendingAuthentication);

            // Clear session data
            session.removeAttribute("conflictEmail");
            session.removeAttribute("existingProvider");
            session.removeAttribute("newProvider");
            session.removeAttribute("pendingPrincipal");
            session.removeAttribute("pendingAuthentication");

            // Redirect to dashboard
            return "redirect:/dashboard";

        } catch (Exception e) {
            log.error("Error during merge confirmation: {}", e.getMessage(), e);
            return "redirect:/merge-confirmation?error=true";
        }
    }
}
