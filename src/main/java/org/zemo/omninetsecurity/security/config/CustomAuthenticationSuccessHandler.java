package org.zemo.omninetsecurity.security.config;

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
import org.zemo.omninetsecurity.security.exception.AccountConflictException;
import org.zemo.omninetsecurity.security.service.UserService;

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
            userService.saveOrUpdateUser(principal);
            response.sendRedirect("http://localhost:5173/dashboard");

        } catch (AccountConflictException e) {
            HttpSession session = request.getSession();
            session.setAttribute("conflictEmail", e.getEmail());
            session.setAttribute("existingProvider", e.getExistingProvider());
            session.setAttribute("newProvider", e.getNewProvider());
            session.setAttribute("pendingPrincipal", principal);
            session.setAttribute("pendingAuthentication", authentication);

            response.sendRedirect("http://localhost:5173/merge-confirmation");

        } catch (Exception e) {
            log.error("Error during authentication success handling: {}", e.getMessage(), e);
            response.sendRedirect("http://localhost:5173/login?error=true");
        }
    }
}
