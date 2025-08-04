package org.zemo.omninetsecurity.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.zemo.omninetsecurity.exception.AccountConflictException;
import org.zemo.omninetsecurity.service.UserService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        try {
            // Try to save or update the user
            userService.saveOrUpdateUser(principal);

            // If successful, redirect to dashboard
            response.sendRedirect("/dashboard");

        } catch (AccountConflictException e) {
            // Account conflict detected - store conflict info in session and redirect to merge page
            log.info("Account conflict detected for email: {}, redirecting to merge confirmation", e.getEmail());

            HttpSession session = request.getSession();
            session.setAttribute("conflictEmail", e.getEmail());
            session.setAttribute("existingProvider", e.getExistingProvider());
            session.setAttribute("newProvider", e.getNewProvider());
            session.setAttribute("pendingPrincipal", principal);
            session.setAttribute("pendingAuthentication", authentication); // Store full authentication

            response.sendRedirect("/merge-confirmation");

        } catch (Exception e) {
            // Other errors - log and redirect to error page
            log.error("Error during authentication success handling: {}", e.getMessage(), e);
            response.sendRedirect("/login?error=true");
        }
    }
}
