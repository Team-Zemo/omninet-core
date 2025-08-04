package org.zemo.omninetsecurity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.zemo.omninetsecurity.exception.AccountConflictException;
import org.zemo.omninetsecurity.model.User;
import org.zemo.omninetsecurity.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountMergeService accountMergeService;

    public User saveOrUpdateUser(OAuth2User principal) {
        return saveOrUpdateUser(principal, false);
    }

    public User saveOrUpdateUser(OAuth2User principal, boolean confirmMerge) {
        String providerId = getUserId(principal);
        String provider = determineProvider(principal);
        String email = principal.getAttribute("email");

        log.info("Processing authentication for Provider ID: {}, Provider: {}, Email: {}", providerId, provider, email);

        // First, check if user exists by provider ID (fastest lookup)
        Optional<User> existingUserById = userRepository.findById(providerId);

        if (existingUserById.isPresent()) {
            User existing = existingUserById.get();
            log.info("Found existing user by ID: {}, updating last login time only", existing.getId());

            // Only update last login time, don't fetch new data from provider
            existing.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(existing);
        }

        // If not found by ID, check for merged accounts by email
        if (email != null && !email.trim().isEmpty()) {
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                User existing = existingUserByEmail.get();

                // If account is merged and contains this provider, update last login only
                if (existing.isAccountMerged() && existing.getLinkedProviders() != null &&
                        existing.getLinkedProviders().contains(provider)) {
                    log.info("Found merged account for email: {}, provider: {}, updating last login time only",
                            email, provider);
                    existing.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(existing);
                }

                // If conflict exists and merge not confirmed, handle conflict
                if (!confirmMerge) {
                    log.warn("Account conflict detected for email: {} between providers: {} and {}",
                            email, existing.getProvider(), provider);
                    throw new AccountConflictException(email, existing.getProvider(), provider);
                }

                // If merge confirmed, merge the accounts
                if (confirmMerge) {
                    User newUserData = extractUserFromPrincipal(principal);
                    return mergeAccounts(existing, newUserData, principal);
                }
            }
        }

        // New user - extract data from provider and save
        log.info("Creating new user from provider data");
        User newUser = extractUserFromPrincipal(principal);
        return userRepository.save(newUser);
    }

    private User updateExistingUser(User existing, User newUserData, OAuth2User principal) {
        existing.setName(newUserData.getName());

        // Handle email updates carefully
        String newEmail = newUserData.getEmail();
        String existingEmail = existing.getEmail();

        if (newEmail != null && !newEmail.equals(existingEmail)) {
            log.info("Email changed from '{}' to '{}' for user {}", existingEmail, newEmail, existing.getId());
            Optional<User> conflictingUser = userRepository.findByEmail(newEmail);
            if (conflictingUser.isEmpty() || conflictingUser.get().getId().equals(existing.getId())) {
                existing.setEmail(newEmail);
                log.info("Updated email for user {}", existing.getId());
            } else {
                log.warn("Email {} already exists for different user, keeping existing email for user {}",
                        newEmail, existing.getId());
            }
        } else if (newEmail != null && existingEmail == null) {
            log.info("Setting email '{}' for user {} (was null)", newEmail, existing.getId());
            existing.setEmail(newEmail);
        }

        existing.setAvatarUrl(newUserData.getAvatarUrl());
        existing.setLastLoginAt(LocalDateTime.now());
        existing.setAttributes(newUserData.getAttributes());

        log.info("Updated existing user: {} with email: {}", existing.getName(), existing.getEmail());
        return userRepository.save(existing);
    }

    private User handleAccountConflict(AccountMergeService.AccountConflict conflict, OAuth2User principal, boolean autoMerge) {
        if (autoMerge) {
            // Automatically merge accounts with existing user as primary
            User primaryUser = conflict.getExistingUser();
            User secondaryUser = conflict.getNewUser();

            log.info("Auto-merging accounts: {} ({}) as primary, {} ({}) as secondary",
                    primaryUser.getName(), primaryUser.getProvider(),
                    secondaryUser.getName(), secondaryUser.getProvider());

            return accountMergeService.mergeAccounts(primaryUser, secondaryUser, principal);
        } else {
            // In a real application, you would redirect to a confirmation page
            // For now, we'll throw a runtime exception to indicate manual intervention needed
            throw new RuntimeException("Account merge confirmation required for email: " + conflict.getExistingUser().getEmail());
        }
    }

    public Map<String, Object> getAccountConflictInfo(OAuth2User principal) {
        User user = extractUserFromPrincipal(principal);
        Optional<AccountMergeService.AccountConflict> conflict = accountMergeService.checkForConflict(user);

        if (conflict.isPresent()) {
            return accountMergeService.createMergeConfirmationData(conflict.get());
        }

        return null;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Map<String, Object> getUserStats() {
        List<User> allUsers = userRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = allUsers.size();
        long githubUsers = allUsers.stream().filter(u -> "github".equals(u.getProvider())).count();
        long googleUsers = allUsers.stream().filter(u -> "google".equals(u.getProvider())).count();
        long mergedAccounts = allUsers.stream().filter(User::isAccountMerged).count();

        stats.put("totalUsers", totalUsers);
        stats.put("githubUsers", githubUsers);
        stats.put("googleUsers", googleUsers);
        stats.put("mergedAccounts", mergedAccounts);
        stats.put("otherProviders", totalUsers - githubUsers - googleUsers);

        return stats;
    }

    public User extractUserFromPrincipal(OAuth2User principal) {
        String id = getUserId(principal);
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String avatarUrl = getAvatarUrl(principal);
        String provider = determineProvider(principal);

        log.info("Extracting user from principal - ID: {}, Email: {}, Name: {}, Provider: {}",
                id, email, name, provider);

        if (email == null || email.trim().isEmpty()) {
            log.warn("User email is null or empty for user ID: {} from provider: {}", id, provider);
        }

        User user = new User(id, email, name, provider);
        user.setAvatarUrl(avatarUrl);
        user.setAttributes(principal.getAttributes());
        user.setLastLoginAt(LocalDateTime.now());

        log.info("Created user object: {}", user);
        return user;
    }

    private String getUserId(OAuth2User principal) {
        Object id = principal.getAttribute("id");
        if (id != null) {
            return id.toString();
        }

        // For Google, use 'sub' claim
        Object sub = principal.getAttribute("sub");
        if (sub != null) {
            return sub.toString();
        }

        return principal.getName();
    }

    private String getAvatarUrl(OAuth2User principal) {
        // GitHub uses 'avatar_url'
        String avatarUrl = principal.getAttribute("avatar_url");
        if (avatarUrl != null) {
            log.info("Found GitHub avatar_url: {}", avatarUrl);
            return avatarUrl;
        }

        // Google uses 'picture'
        String picture = principal.getAttribute("picture");
        if (picture != null) {
            log.info("Found Google picture: {}", picture);
            return picture;
        }

        log.warn("No avatar URL found for user");
        return null;
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

    private User mergeAccounts(User primaryUser, User secondaryUser, OAuth2User principal) {
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

        // Save merged primary user
        return userRepository.save(primaryUser);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
