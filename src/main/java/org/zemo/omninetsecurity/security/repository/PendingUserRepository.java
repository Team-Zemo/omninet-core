package org.zemo.omninetsecurity.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zemo.omninetsecurity.security.model.PendingUser;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, String> {
    Optional<PendingUser> findByEmail(String email);
    Optional<PendingUser> findByVerificationToken(String verificationToken);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    void deleteByEmail(String email);
}
