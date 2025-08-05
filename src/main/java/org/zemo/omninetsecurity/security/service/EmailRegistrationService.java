package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninetsecurity.security.exception.AccountConflictException;
import org.zemo.omninetsecurity.security.model.EmailVerification;
import org.zemo.omninetsecurity.security.model.PendingUser;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.repository.EmailVerificationRepository;
import org.zemo.omninetsecurity.security.repository.PendingUserRepository;
import org.zemo.omninetsecurity.security.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRegistrationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Map<String, Object> initiateEmailRegistration(String email) {
        log.info("Initiating email registration for: {}", email);

        // Clean up expired records
        cleanupExpiredRecords(email);

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if ("email".equals(user.getRegistrationSource())) {
                throw new RuntimeException("An account with this email already exists. Please try logging in.");
            } else {
                // OAuth user exists - will handle merge later
                log.info("OAuth user exists for email: {}, will handle merge during completion", email);
            }
        }

        // Generate OTP
        String otp = generateOtp();

        // Save verification record
        EmailVerification verification = new EmailVerification(email, otp, 15); // 15 minutes validity
        emailVerificationRepository.save(verification);

        // Send OTP email
        emailService.sendOtpEmail(email, otp);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Verification code sent to your email");
        response.put("email", email);

        return response;
    }

    @Transactional
    public Map<String, Object> verifyOtp(String email, String otp) {
        log.info("Verifying OTP for email: {}", email);

        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndOtpAndVerifiedFalseAndUsedFalse(email, otp);

        if (verificationOpt.isEmpty()) {
            throw new RuntimeException("Invalid or expired verification code");
        }

        EmailVerification verification = verificationOpt.get();

        // Increment attempt count
        verification.setAttemptCount(verification.getAttemptCount() + 1);

        if (!verification.isValid()) {
            emailVerificationRepository.save(verification);
            throw new RuntimeException("Verification code has expired or maximum attempts exceeded");
        }

        // Mark as verified
        verification.setVerified(true);
        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        // Generate verification token for the next step
        String verificationToken = UUID.randomUUID().toString();

        // Check for existing OAuth user
        Optional<User> existingUser = userRepository.findByEmail(email);
        boolean hasConflict = existingUser.isPresent() && !"email".equals(existingUser.get().getRegistrationSource());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Email verified successfully");
        response.put("verificationToken", verificationToken);
        response.put("email", email);
        response.put("hasConflict", hasConflict);

        if (hasConflict) {
            User user = existingUser.get();
            response.put("existingProvider", user.getProvider());
            response.put("conflictMessage", String.format(
                "An account with this email already exists using %s authentication. " +
                "Completing registration will merge your accounts.", user.getProvider()));
        }

        // Store verification token temporarily - properly initialize PendingUser
        PendingUser pendingUser = new PendingUser();
        pendingUser.setEmail(email);
        pendingUser.setVerificationToken(verificationToken);
        pendingUser.setEmailVerified(true);
        pendingUser.setCreatedAt(LocalDateTime.now());
        pendingUser.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours to complete registration
        pendingUserRepository.save(pendingUser);

        return response;
    }

    @Transactional
    public Map<String, Object> completeRegistration(String email, String name, String password,
                                                   String verificationToken, boolean confirmMerge) {
        log.info("Completing registration for email: {}", email);

        // Validate verification token
        Optional<PendingUser> pendingUserOpt = pendingUserRepository.findByVerificationToken(verificationToken);
        if (pendingUserOpt.isEmpty() || !pendingUserOpt.get().getEmail().equals(email)) {
            throw new RuntimeException("Invalid verification token");
        }

        PendingUser pendingUser = pendingUserOpt.get();
        if (pendingUser.isExpired() || !pendingUser.isEmailVerified()) {
            throw new RuntimeException("Verification token has expired");
        }

        // Password is now required for ALL registrations
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password is required for all registrations");
        }

        // Check for existing OAuth user
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if ("email".equals(existingUser.getRegistrationSource())) {
                throw new RuntimeException("An account with this email already exists");
            }

            // OAuth user exists - handle merge (always require password now)
            if (!confirmMerge) {
                throw new AccountConflictException(email, existingUser.getProvider(), "email");
            }

            return mergeWithOAuthAccount(existingUser, name, password, pendingUser);
        } else {
            // Create new email-based user
            return createNewEmailUser(email, name, password, pendingUser);
        }
    }

    private Map<String, Object> createNewEmailUser(String email, String name, String password, PendingUser pendingUser) {
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, name, hashedPassword);
        user = userRepository.save(user);

        // Clean up pending user record
        pendingUserRepository.delete(pendingUser);

        // Send welcome email
        emailService.sendWelcomeEmail(email, name);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Registration completed successfully");
        response.put("user", user);
        response.put("merged", false);

        log.info("Created new email user: {}", email);
        return response;
    }

    private Map<String, Object> mergeWithOAuthAccount(User existingUser, String name, String password, PendingUser pendingUser) {
        // Add password to existing OAuth user
        existingUser.setPassword(passwordEncoder.encode(password));

        // Update name if provided and current name is different
        if (name != null && !name.trim().isEmpty() && !name.equals(existingUser.getName())) {
            existingUser.setName(name);
        }

        // Update linked providers
        String linkedProviders = existingUser.getLinkedProviders();
        if (linkedProviders == null || !linkedProviders.contains("email")) {
            linkedProviders = linkedProviders != null ? linkedProviders + ",email" : "email";
            existingUser.setLinkedProviders(linkedProviders);
        }

        existingUser.setAccountMerged(true);
        existingUser.setLastLoginAt(LocalDateTime.now());

        existingUser = userRepository.save(existingUser);

        // Clean up pending user record
        pendingUserRepository.delete(pendingUser);

        // Send merge notification
        emailService.sendAccountMergeNotification(existingUser.getEmail(), existingUser.getName(), "email");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Account merged successfully");
        response.put("user", existingUser);
        response.put("merged", true);
        response.put("mergedProviders", existingUser.getLinkedProviders());

        log.info("Merged email with OAuth account: {}", existingUser.getEmail());
        return response;
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void cleanupExpiredRecords(String email) {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.deleteByEmailAndExpiresAtBefore(email, now);
        // Also cleanup old pending users for this email
        pendingUserRepository.deleteByEmail(email);
    }

    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.deleteByExpiresAtBefore(now);
        pendingUserRepository.deleteByExpiresAtBefore(now);
        log.info("Cleaned up expired verification records");
    }
}
