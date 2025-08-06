package org.zemo.omninetsecurity.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.zemo.omninetsecurity.security.exception.AccountConflictException;
import org.zemo.omninetsecurity.security.model.RefreshToken;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.service.JwtService;
import org.zemo.omninetsecurity.security.service.RefreshTokenService;
import org.zemo.omninetsecurity.security.service.UserService;
import org.zemo.omninetsecurity.security.util.HttpUtils;

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
                                      Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = HttpUtils.getClientIpAddress(request);

        try {
            User user = userService.saveOrUpdateUser(principal, true);

            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/callback")
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
