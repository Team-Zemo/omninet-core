package org.zemo.omninet.security.config;

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
import org.springframework.web.util.UriComponentsBuilder;
import org.zemo.omninet.security.exception.AccountConflictException;
import org.zemo.omninet.security.model.RefreshToken;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.security.service.JwtService;
import org.zemo.omninet.security.service.RefreshTokenService;
import org.zemo.omninet.security.service.UserService;
import org.zemo.omninet.security.util.HttpUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = HttpUtils.getClientIpAddress(request);

        try {
            User user = userService.saveOrUpdateUser(principal, true);

            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            // 1. Get the redirect URI from the session
            HttpSession session = request.getSession();
            String targetUrl = (String) session.getAttribute("OAUTH2_REDIRECT_URI");

            // 2. Clear the attribute
            if (targetUrl != null) {
                System.out.println("Redirecting to: " + targetUrl);
                session.removeAttribute("OAUTH2_REDIRECT_URI");
            } else {
                // 3. Fallback to a default URL if none was provided
                targetUrl = "http://localhost:5173/auth/callback"; // Default for web
            }

            String redirectUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("access_token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refresh_token", URLEncoder.encode(refreshToken.getToken(), StandardCharsets.UTF_8))
                    .queryParam("token_type", "Bearer")
                    .queryParam("expires_in", jwtService.getAccessTokenExpiration())
                    .build()
                    .toUriString();

            response.sendRedirect(redirectUrl);

        } catch (AccountConflictException e) {
            String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/conflict")
                    .queryParam("email", URLEncoder.encode(e.getEmail(), StandardCharsets.UTF_8))
                    .queryParam("existing_provider", URLEncoder.encode(e.getExistingProvider(), StandardCharsets.UTF_8))
                    .queryParam("new_provider", URLEncoder.encode(e.getNewProvider(), StandardCharsets.UTF_8))
                    .build()
                    .toUriString();

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error during OAuth2 authentication success handling: {}", e.getMessage(), e);
            response.sendRedirect("http://localhost:5173/login?error=oauth_error");
        }
    }
}
