package org.zemo.omninet.security.config;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if ("github".equals(registrationId)) {
            String currentEmail = user.getAttribute("email");

            if (currentEmail == null || currentEmail.trim().isEmpty()) {
                String email = fetchGitHubUserEmail(userRequest.getAccessToken().getTokenValue());
                if (email != null) {
                    Map<String, Object> attributes = new HashMap<>(user.getAttributes());
                    attributes.put("email", email);

                    return new DefaultOAuth2User(
                            user.getAuthorities(),
                            attributes,
                            userRequest.getClientRegistration().getProviderDetails()
                                    .getUserInfoEndpoint().getUserNameAttributeName()
                    );
                }
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
                for (Object emailObj : response.getBody()) {
                    if (emailObj instanceof Map) {
                        Map<String, Object> emailData = (Map<String, Object>) emailObj;
                        Boolean isPrimary = (Boolean) emailData.get("primary");
                        Boolean isVerified = (Boolean) emailData.get("verified");
                        String email = (String) emailData.get("email");

                        if (Boolean.TRUE.equals(isPrimary) && Boolean.TRUE.equals(isVerified)) {
                            return email;
                        }
                    }
                }

                for (Object emailObj : response.getBody()) {
                    if (emailObj instanceof Map) {
                        Map<String, Object> emailData = (Map<String, Object>) emailObj;
                        Boolean isVerified = (Boolean) emailData.get("verified");
                        String email = (String) emailData.get("email");

                        if (Boolean.TRUE.equals(isVerified)) {
                            return email;
                        }
                    }
                }

                Object firstEmailObj = response.getBody().getFirst();
                if (firstEmailObj instanceof Map) {
                    Map<String, Object> emailData = (Map<String, Object>) firstEmailObj;
                    return (String) emailData.get("email");
                }
            }
        } catch (Exception e) {
            log.error("Error fetching GitHub user email: {}", e.getMessage(), e);
        }

        return null;
    }
}
