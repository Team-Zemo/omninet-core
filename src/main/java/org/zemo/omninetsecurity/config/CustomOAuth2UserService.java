package org.zemo.omninetsecurity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        // Check if this is a GitHub user and email is missing or null
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if ("github".equals(registrationId)) {
            String currentEmail = user.getAttribute("email");
            log.info("GitHub user email from /user endpoint: {}", currentEmail);

            if (currentEmail == null || currentEmail.trim().isEmpty()) {
                log.info("GitHub user email is null/empty, fetching from /user/emails endpoint");

                String email = fetchGitHubUserEmail(userRequest.getAccessToken().getTokenValue());
                if (email != null) {
                    log.info("Successfully fetched GitHub email: {}", email);
                    // Create a new user with the email attribute added
                    Map<String, Object> attributes = new HashMap<>(user.getAttributes());
                    attributes.put("email", email);

                    return new DefaultOAuth2User(
                        user.getAuthorities(),
                        attributes,
                        userRequest.getClientRegistration().getProviderDetails()
                            .getUserInfoEndpoint().getUserNameAttributeName()
                    );
                } else {
                    log.warn("Failed to fetch GitHub email from /user/emails endpoint");
                }
            } else {
                log.info("GitHub user already has email: {}", currentEmail);
            }
        }

        return user;
    }

    private String fetchGitHubUserEmail(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Omninet-Security-App");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<List> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                entity,
                List.class
            );

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                log.info("Received {} email(s) from GitHub /user/emails endpoint", response.getBody().size());

                // Find the primary email first
                for (Object emailObj : response.getBody()) {
                    if (emailObj instanceof Map) {
                        Map<String, Object> emailData = (Map<String, Object>) emailObj;
                        Boolean isPrimary = (Boolean) emailData.get("primary");
                        Boolean isVerified = (Boolean) emailData.get("verified");
                        String email = (String) emailData.get("email");

                        log.debug("Email: {}, Primary: {}, Verified: {}", email, isPrimary, isVerified);

                        if (Boolean.TRUE.equals(isPrimary) && Boolean.TRUE.equals(isVerified)) {
                            log.info("Found primary verified email: {}", email);
                            return email;
                        }
                    }
                }

                // If no primary verified email found, use the first verified one
                for (Object emailObj : response.getBody()) {
                    if (emailObj instanceof Map) {
                        Map<String, Object> emailData = (Map<String, Object>) emailObj;
                        Boolean isVerified = (Boolean) emailData.get("verified");
                        String email = (String) emailData.get("email");

                        if (Boolean.TRUE.equals(isVerified)) {
                            log.info("Found verified email: {}", email);
                            return email;
                        }
                    }
                }

                // Last resort: use the first email even if not verified
                Object firstEmailObj = response.getBody().get(0);
                if (firstEmailObj instanceof Map) {
                    Map<String, Object> emailData = (Map<String, Object>) firstEmailObj;
                    String email = (String) emailData.get("email");
                    log.warn("Using first available email (may not be verified): {}", email);
                    return email;
                }
            } else {
                log.warn("No emails returned from GitHub /user/emails endpoint");
            }
        } catch (Exception e) {
            log.error("Error fetching GitHub user email: {}", e.getMessage(), e);
        }

        return null;
    }
}
