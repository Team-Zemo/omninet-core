package org.zemo.omninetsecurity.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.security.dto.ApiResponse;
import org.zemo.omninetsecurity.security.dto.EmailLoginRequest;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.service.AuthenticationService;
import org.zemo.omninetsecurity.security.service.UserService;
import org.zemo.omninetsecurity.security.util.HttpUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001","http://localhost:5173"})
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        user = userService.saveUser(user);

        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithEmail(
            @Valid @RequestBody EmailLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = HttpUtils.getClientIpAddress(httpRequest);

            ApiResponse<Map<String, Object>> response = authenticationService.authenticateUser(
                request.getEmail(),
                request.getPassword(),
                userAgent,
                ipAddress
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error during email login: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Login failed", e.getMessage())
            );
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Refresh token is required")
                );
            }

            ApiResponse<Map<String, Object>> response = authenticationService.refreshToken(refreshToken);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Token refresh failed")
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            ApiResponse<Map<String, Object>> response = authenticationService.logout(refreshToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Logout failed")
            );
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logoutAll(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
            }

            ApiResponse<Map<String, Object>> response = authenticationService.logoutAll(user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during logout all: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Logout from all devices failed")
            );
        }
    }

    @GetMapping("/check-methods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuthenticationMethods(@RequestParam String email) {
        try {
            ApiResponse<Map<String, Object>> response = authenticationService.checkAuthenticationMethods(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking authentication methods: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Error checking authentication methods")
            );
        }
    }

    @PostMapping("/add-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addPasswordToAccount(
            @RequestParam String email,
            @RequestParam String password) {
        try {
            ApiResponse<Map<String, Object>> response = authenticationService.addPasswordToOAuthAccount(email, password);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error adding password to account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to add password authentication")
            );
        }
    }
}
