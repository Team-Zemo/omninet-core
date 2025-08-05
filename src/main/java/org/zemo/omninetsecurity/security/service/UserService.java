package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.zemo.omninetsecurity.security.exception.AccountConflictException;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.repository.UserRepository;

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

    public User saveOrUpdateUser(OAuth2User principal) {
        return saveOrUpdateUser(principal, false);
    }

    public User saveOrUpdateUser(OAuth2User principal, boolean confirmMerge) {
        String providerId = getUserId(principal);
        String provider = determineProvider(principal);
        String email = principal.getAttribute("email");

        Optional<User> existingUserById = userRepository.findById(providerId);

        if (existingUserById.isPresent()) {
            User existing = existingUserById.get();
            existing.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(existing);
        }

        if (email != null && !email.trim().isEmpty()) {
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                User existing = existingUserByEmail.get();

                if (existing.isAccountMerged() && existing.getLinkedProviders() != null &&
                        existing.getLinkedProviders().contains(provider)) {
                    existing.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(existing);
                }

                if (!confirmMerge) {
                    throw new AccountConflictException(email, existing.getProvider(), provider);
                } else {
                    User newUserData = extractUserFromPrincipal(principal);
                    return mergeAccounts(existing, newUserData, principal);
                }
            }
        }

        User newUser = extractUserFromPrincipal(principal);
        return userRepository.save(newUser);
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

        User user = new User(id, email, name, provider);
        user.setAvatarUrl(avatarUrl);
        user.setLastLoginAt(LocalDateTime.now());

        return user;
    }

    private String getUserId(OAuth2User principal) {
        Object id = principal.getAttribute("id");
        if (id != null) {
            return id.toString();
        }

        Object sub = principal.getAttribute("sub");
        if (sub != null) {
            return sub.toString();
        }

        return principal.getName();
    }

    private String getAvatarUrl(OAuth2User principal) {
        String avatarUrl = principal.getAttribute("avatar_url");
        if (avatarUrl != null) {
            return avatarUrl;
        }

        return principal.getAttribute("picture");
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

    private User mergeAccounts(User primaryUser, User secondaryUser, OAuth2User principal) {
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

        return userRepository.save(primaryUser);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
