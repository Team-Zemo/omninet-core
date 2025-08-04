package org.zemo.omninetsecurity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMergeService {

    private final UserRepository userRepository;

    public static class AccountConflict {
        private final User existingUser;
        private final User newUser;
        private final String conflictType;

        public AccountConflict(User existingUser, User newUser, String conflictType) {
            this.existingUser = existingUser;
            this.newUser = newUser;
            this.conflictType = conflictType;
        }

        public User getExistingUser() {
            return existingUser;
        }

        public User getNewUser() {
            return newUser;
        }

        public String getConflictType() {
            return conflictType;
        }
    }

    /**
     * Check if there's a conflict when trying to save a new user
     */
    public Optional<AccountConflict> checkForConflict(User newUser) {
        if (newUser.getEmail() == null || newUser.getEmail().trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(newUser.getEmail());

        if (existingUserByEmail.isPresent()) {
            User existing = existingUserByEmail.get();

            // If same provider and same ID, no conflict
            if (existing.getId().equals(newUser.getId()) &&
                    existing.getProvider().equals(newUser.getProvider())) {
                return Optional.empty();
            }

            // If the existing account is already merged and contains the new provider, no conflict
            if (existing.isAccountMerged() && existing.getLinkedProviders() != null &&
                    existing.getLinkedProviders().contains(newUser.getProvider())) {
                log.info("Account already merged for email: {}, provider {} is already linked",
                        newUser.getEmail(), newUser.getProvider());
                return Optional.empty();
            }

            // If different provider but same email, we have a conflict
            if (!existing.getProvider().equals(newUser.getProvider())) {
                return Optional.of(new AccountConflict(existing, newUser, "DIFFERENT_PROVIDER"));
            }

            // If same provider but different ID, also a conflict
            if (existing.getProvider().equals(newUser.getProvider()) &&
                    !existing.getId().equals(newUser.getId())) {
                return Optional.of(new AccountConflict(existing, newUser, "SAME_PROVIDER_DIFFERENT_ID"));
            }
        }

        return Optional.empty();
    }

    /**
     * Merge two user accounts - keep the first one as primary
     */
    public User mergeAccounts(User primaryUser, User secondaryUser, OAuth2User currentPrincipal) {
        log.info("Merging accounts: Primary {} ({}), Secondary {} ({})",
                primaryUser.getName(), primaryUser.getProvider(),
                secondaryUser.getName(), secondaryUser.getProvider());

        // Update primary user with merged information
        primaryUser.setAccountMerged(true);

        // Add secondary provider to linked providers
        String linkedProviders = primaryUser.getLinkedProviders();
        if (linkedProviders == null || linkedProviders.trim().isEmpty()) {
            linkedProviders = primaryUser.getProvider();
        }

        if (!linkedProviders.contains(secondaryUser.getProvider())) {
            linkedProviders += "," + secondaryUser.getProvider();
            primaryUser.setLinkedProviders(linkedProviders);
        }

        // Update last login time
        primaryUser.setLastLoginAt(LocalDateTime.now());

        // Merge attributes from current login session
        if (currentPrincipal != null) {
            Map<String, Object> mergedAttributes = new HashMap<>();
            if (primaryUser.getAttributes() != null) {
                mergedAttributes.putAll(primaryUser.getAttributes());
            }

            // Add current session attributes with provider prefix
            String providerPrefix = determineProvider(currentPrincipal) + "_";
            currentPrincipal.getAttributes().forEach((key, value) -> {
                mergedAttributes.put(providerPrefix + key, value);
            });

            primaryUser.setAttributes(mergedAttributes);
        }

        // Update avatar if secondary user has one and primary doesn't
        if ((primaryUser.getAvatarUrl() == null || primaryUser.getAvatarUrl().trim().isEmpty())
                && secondaryUser.getAvatarUrl() != null) {
            primaryUser.setAvatarUrl(secondaryUser.getAvatarUrl());
        }

        // Update name if primary user's name is empty but secondary has one
        if ((primaryUser.getName() == null || primaryUser.getName().trim().isEmpty())
                && secondaryUser.getName() != null && !secondaryUser.getName().trim().isEmpty()) {
            primaryUser.setName(secondaryUser.getName());
        }

        // Save merged primary user
        User savedUser = userRepository.save(primaryUser);

        // DON'T save the secondary user as a new record to avoid unique constraint violation
        // Instead, just log the merge operation
        log.info("Successfully merged accounts. Primary user: {}, Added provider: {}",
                savedUser.getId(), secondaryUser.getProvider());

        return savedUser;
    }

    /**
     * Create merge confirmation data for frontend
     */
    public Map<String, Object> createMergeConfirmationData(AccountConflict conflict) {
        Map<String, Object> data = new HashMap<>();

        User existing = conflict.getExistingUser();
        User newUser = conflict.getNewUser();

        data.put("conflictType", conflict.getConflictType());
        data.put("email", existing.getEmail());

        // Existing account info
        Map<String, Object> existingInfo = new HashMap<>();
        existingInfo.put("id", existing.getId());
        existingInfo.put("name", existing.getName());
        existingInfo.put("provider", existing.getProvider());
        existingInfo.put("createdAt", existing.getCreatedAt());
        existingInfo.put("linkedProviders", existing.getLinkedProviders());
        data.put("existingAccount", existingInfo);

        // New account info
        Map<String, Object> newInfo = new HashMap<>();
        newInfo.put("id", newUser.getId());
        newInfo.put("name", newUser.getName());
        newInfo.put("provider", newUser.getProvider());
        data.put("newAccount", newInfo);

        return data;
    }

    private String determineProvider(OAuth2User principal) {
        // GitHub specific attributes
        if (principal.getAttribute("login") != null) {
            return "github";
        }

        // Google specific attributes
        if (principal.getAttribute("given_name") != null ||
                principal.getAttribute("family_name") != null) {
            return "google";
        }

        // Check issuer for Google
        String iss = principal.getAttribute("iss");
        if (iss != null && iss.contains("accounts.google.com")) {
            return "google";
        }

        return "unknown";
    }
}
