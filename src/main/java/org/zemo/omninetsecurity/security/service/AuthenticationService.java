package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninetsecurity.security.dto.ApiResponse;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiResponse<Map<String, Object>> authenticateUser(String email, String password) {
        try {
            log.info("Attempting email/password authentication for: {}", email);

            // Find user by email
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ApiResponse.error("Invalid email or password");
            }

            User user = userOpt.get();

            // Check if user has a password set
            if (!user.hasPassword()) {
                return ApiResponse.error("Password authentication not available for this account. Please use " +
                    user.getProvider() + " to sign in, or set up password authentication.");
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ApiResponse.error("Invalid email or password");
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);

            // Create response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            responseData.put("authMethod", "email");
            responseData.put("hasMultipleProviders", user.isAccountMerged());

            return ApiResponse.success(responseData, "Authentication successful");

        } catch (Exception e) {
            log.error("Error during email/password authentication: {}", e.getMessage(), e);
            return ApiResponse.error("Authentication failed");
        }
    }

    public ApiResponse<Map<String, Object>> checkAuthenticationMethods(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            Map<String, Object> methods = new HashMap<>();

            if (userOpt.isEmpty()) {
                methods.put("emailPassword", false);
                methods.put("oauth", false);
                methods.put("providers", new String[0]);
                methods.put("canRegister", true);
            } else {
                User user = userOpt.get();
                methods.put("emailPassword", user.hasPassword());
                methods.put("oauth", !"email".equals(user.getRegistrationSource()));
                methods.put("providers", user.getLinkedProviders().split(","));
                methods.put("canRegister", false);
                methods.put("accountExists", true);
            }

            return ApiResponse.success(methods, "Authentication methods retrieved");

        } catch (Exception e) {
            log.error("Error checking authentication methods: {}", e.getMessage(), e);
            return ApiResponse.error("Error checking authentication methods");
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> addPasswordToOAuthAccount(String email, String password) {
        try {
            log.info("Adding password to OAuth account: {}", email);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ApiResponse.error("Account not found");
            }

            User user = userOpt.get();

            // Check if user already has a password
            if (user.hasPassword()) {
                return ApiResponse.error("Account already has password authentication enabled");
            }

            // Add password to the account
            user.setPassword(passwordEncoder.encode(password));

            // Update linked providers to include email
            String linkedProviders = user.getLinkedProviders();
            if (linkedProviders == null || !linkedProviders.contains("email")) {
                linkedProviders = linkedProviders != null ? linkedProviders + ",email" : "email";
                user.setLinkedProviders(linkedProviders);
            }

            user.setAccountMerged(true);
            user = userRepository.save(user);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            responseData.put("message", "Password authentication added successfully");
            responseData.put("availableProviders", user.getLinkedProviders());

            return ApiResponse.success(responseData, "Password added to OAuth account");

        } catch (Exception e) {
            log.error("Error adding password to OAuth account: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to add password authentication");
        }
    }
}
