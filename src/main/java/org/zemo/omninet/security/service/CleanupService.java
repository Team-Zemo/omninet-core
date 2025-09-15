package org.zemo.omninet.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.zemo.omninet.security.repository.EmailVerificationRepository;
import org.zemo.omninet.security.repository.PendingUserRepository;
import org.zemo.omninet.security.repository.RefreshTokenRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final PendingUserRepository pendingUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // Runs once after the application is fully started to avoid race with schema creation
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            performCleanup();
        } catch (Exception e) {
            log.warn("Initial cleanup failed (will be retried by scheduled job): {}", e.getMessage());
        }
    }

    // Configurable initial delay to allow Hibernate/DDL to finish. Defaults shown here (10s start, 1h repeat).
    @Scheduled(initialDelayString = "${cleanup.initial-delay-ms:10000}", fixedDelayString = "${cleanup.fixed-delay-ms:3600000}")
    public void scheduledCleanup() {
        try {
            performCleanup();
        } catch (Exception e) {
            log.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void performCleanup() {
        LocalDateTime now = LocalDateTime.now();
        try {
            emailVerificationRepository.deleteByExpiresAtBefore(now);
            pendingUserRepository.deleteByExpiresAtBefore(now);
            refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(now);
            log.info("Cleaned up expired verification, pending user and refresh token records");
        } catch (InvalidDataAccessResourceUsageException ex) {
            // Typical when DB schema/tables are not yet present; log and let scheduled job retry later
            log.warn("Database not ready for cleanup yet: {}", ex.getMessage());
        }
    }
}
