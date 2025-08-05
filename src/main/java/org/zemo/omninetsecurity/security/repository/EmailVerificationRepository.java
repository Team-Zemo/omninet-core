package org.zemo.omninetsecurity.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zemo.omninetsecurity.security.model.EmailVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    Optional<EmailVerification> findByEmailAndOtpAndVerifiedFalseAndUsedFalse(String email, String otp);
    List<EmailVerification> findByEmailAndVerifiedFalseAndUsedFalse(String email);
    void deleteByEmailAndExpiresAtBefore(String email, LocalDateTime dateTime);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
