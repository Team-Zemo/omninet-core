package org.zemo.omninetsecurity.security.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninetsecurity.security.dto.CompleteRegistrationRequest;
import org.zemo.omninetsecurity.security.dto.EmailRegistrationRequest;
import org.zemo.omninetsecurity.security.dto.OtpVerificationRequest;
import org.zemo.omninetsecurity.security.exception.AccountConflictException;
import org.zemo.omninetsecurity.security.service.EmailRegistrationService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
@RequiredArgsConstructor
@Validated
@Slf4j
public class EmailRegistrationController {

    private final EmailRegistrationService emailRegistrationService;

    @PostMapping("/register/initiate")
    public ResponseEntity<Map<String, Object>> initiateRegistration(@Valid @RequestBody EmailRegistrationRequest request) {
        try {
            Map<String, Object> response = emailRegistrationService.initiateEmailRegistration(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating registration: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        try {
            Map<String, Object> response = emailRegistrationService.verifyOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error verifying OTP: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/register/complete")
    public ResponseEntity<Map<String, Object>> completeRegistration(
            @Valid @RequestBody CompleteRegistrationRequest request,
            @RequestParam(defaultValue = "false") boolean confirmMerge) {
        try {
            Map<String, Object> response = emailRegistrationService.completeRegistration(
                    request.getEmail(),
                    request.getName(),
                    request.getPassword(),
                    request.getVerificationToken(),
                    confirmMerge
            );
            return ResponseEntity.ok(response);
        } catch (AccountConflictException e) {
            log.info("Account conflict detected during registration: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("conflict", true);
            errorResponse.put("email", e.getEmail());
            errorResponse.put("existingProvider", e.getExistingProvider());
            errorResponse.put("newProvider", e.getNewProvider());
            errorResponse.put("message", String.format(
                "An account with email %s already exists using %s authentication. " +
                "Do you want to merge your accounts?", e.getEmail(), e.getExistingProvider()));
            return ResponseEntity.status(409).body(errorResponse);
        } catch (Exception e) {
            log.error("Error completing registration: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@Valid @RequestBody EmailRegistrationRequest request) {
        try {
            // Reuse the initiate registration flow to resend OTP
            Map<String, Object> response = emailRegistrationService.initiateEmailRegistration(request.getEmail());
            response.put("message", "Verification code resent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resending OTP: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/register/check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestParam String email) {
        try {
            Map<String, Object> response = new HashMap<>();
            // This is a simple check - the actual conflict resolution happens during completion
            response.put("available", true);
            response.put("message", "Email can be used for registration");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking email availability: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("available", false);
            errorResponse.put("message", "Error checking email availability");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
