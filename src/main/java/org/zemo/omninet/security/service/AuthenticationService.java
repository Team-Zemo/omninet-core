package org.zemo.omninet.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninet.security.dto.ApiResponse;
import org.zemo.omninet.security.model.RefreshToken;
import org.zemo.omninet.security.model.User;
import org.zemo.omninet.security.repository.UserRepository;

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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public ApiResponse<Map<String, Object>> authenticateUser(String email, String password, String userAgent, String ipAddress) {
        try {
            log.info("Attempting email/password authentication for: {}", email);

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ApiResponse.error("Invalid email or password");
            }

            User user = userOpt.get();

            if (!user.hasPassword()) {
                return ApiResponse.error("Password authentication not available for this account. Please use " +
                        user.getProvider() + " to sign in, or set up password authentication.");
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ApiResponse.error("Invalid email or password");
            }

            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken.getToken());
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", jwtService.getAccessTokenExpiration());
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

            if (user.hasPassword()) {
                return ApiResponse.error("Account already has password authentication enabled");
            }

            user.setPassword(passwordEncoder.encode(password));

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

    public ApiResponse<Map<String, Object>> refreshToken(String refreshTokenString) {
        try {
            Optional<String> newAccessToken = refreshTokenService.refreshAccessToken(refreshTokenString);

            if (newAccessToken.isEmpty()) {
                return ApiResponse.error("Invalid or expired refresh token");
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", newAccessToken.get());
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", jwtService.getAccessTokenExpiration());

            return ApiResponse.success(responseData, "Token refreshed successfully");

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to refresh token");
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> logout(String refreshTokenString) {
        try {
            if (refreshTokenString != null && !refreshTokenString.trim().isEmpty()) {
                refreshTokenService.revokeToken(refreshTokenString);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Logged out successfully");

            return ApiResponse.success(responseData, "Logout successful");

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return ApiResponse.error("Logout failed");
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> logoutAll(String userId) {
        try {
            refreshTokenService.revokeAllUserTokens(userId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Logged out from all devices successfully");

            return ApiResponse.success(responseData, "Logout from all devices successful");

        } catch (Exception e) {
            log.error("Error during logout all: {}", e.getMessage(), e);
            return ApiResponse.error("Logout from all devices failed");
        }
    }
}
