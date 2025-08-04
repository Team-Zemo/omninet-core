package org.zemo.omninetsecurity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.repository.UserRepository;

import java.time.LocalDateTime;
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

    public Optional<AccountConflict> checkForConflict(User newUser) {
        if (newUser.getEmail() == null || newUser.getEmail().trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(newUser.getEmail());

        if (existingUserByEmail.isPresent()) {
            User existing = existingUserByEmail.get();

            if (existing.getId().equals(newUser.getId()) &&
                    existing.getProvider().equals(newUser.getProvider())) {
                return Optional.empty();
            }

            if (existing.isAccountMerged() && existing.getLinkedProviders() != null &&
                    existing.getLinkedProviders().contains(newUser.getProvider())) {
                return Optional.empty();
            }

            if (!existing.getProvider().equals(newUser.getProvider())) {
                return Optional.of(new AccountConflict(existing, newUser, "DIFFERENT_PROVIDER"));
            }

            if (existing.getProvider().equals(newUser.getProvider()) &&
                    !existing.getId().equals(newUser.getId())) {
                return Optional.of(new AccountConflict(existing, newUser, "SAME_PROVIDER_DIFFERENT_ID"));
            }
        }

        return Optional.empty();
    }

    public User mergeAccounts(User primaryUser, User secondaryUser, OAuth2User currentPrincipal) {
        primaryUser.setAccountMerged(true);

        String linkedProviders = primaryUser.getLinkedProviders();
        if (linkedProviders == null || linkedProviders.trim().isEmpty()) {
            linkedProviders = primaryUser.getProvider();
        }

        if (!linkedProviders.contains(secondaryUser.getProvider())) {
            linkedProviders += "," + secondaryUser.getProvider();
            primaryUser.setLinkedProviders(linkedProviders);
        }

        primaryUser.setLastLoginAt(LocalDateTime.now());

        if (currentPrincipal != null) {
            Map<String, Object> mergedAttributes = new HashMap<>();
            if (primaryUser.getAttributes() != null) {
                mergedAttributes.putAll(primaryUser.getAttributes());
            }

            String providerPrefix = determineProvider(currentPrincipal) + "_";
            currentPrincipal.getAttributes().forEach((key, value) -> {
                mergedAttributes.put(providerPrefix + key, value);
            });

            primaryUser.setAttributes(mergedAttributes);
        }

        if ((primaryUser.getAvatarUrl() == null || primaryUser.getAvatarUrl().trim().isEmpty())
                && secondaryUser.getAvatarUrl() != null) {
            primaryUser.setAvatarUrl(secondaryUser.getAvatarUrl());
        }

        if ((primaryUser.getName() == null || primaryUser.getName().trim().isEmpty())
                && secondaryUser.getName() != null && !secondaryUser.getName().trim().isEmpty()) {
            primaryUser.setName(secondaryUser.getName());
        }

        return userRepository.save(primaryUser);
    }

    public Map<String, Object> createMergeConfirmationData(AccountConflict conflict) {
        Map<String, Object> data = new HashMap<>();

        User existing = conflict.getExistingUser();
        User newUser = conflict.getNewUser();

        data.put("conflictType", conflict.getConflictType());
        data.put("email", existing.getEmail());

        Map<String, Object> existingInfo = new HashMap<>();
        existingInfo.put("id", existing.getId());
        existingInfo.put("name", existing.getName());
        existingInfo.put("provider", existing.getProvider());
        existingInfo.put("createdAt", existing.getCreatedAt());
        existingInfo.put("linkedProviders", existing.getLinkedProviders());
        data.put("existingAccount", existingInfo);

        Map<String, Object> newInfo = new HashMap<>();
        newInfo.put("id", newUser.getId());
        newInfo.put("name", newUser.getName());
        newInfo.put("provider", newUser.getProvider());
        data.put("newAccount", newInfo);

        return data;
    }

    private String determineProvider(OAuth2User principal) {
        if (principal.getAttribute("login") != null) {
            return "github";
        }

        if (principal.getAttribute("given_name") != null ||
                principal.getAttribute("family_name") != null) {
            return "google";
        }

        String iss = principal.getAttribute("iss");
        if (iss != null && iss.contains("accounts.google.com")) {
            return "google";
        }

        return "unknown";
    }
}
