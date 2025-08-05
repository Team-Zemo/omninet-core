package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final EmailRegistrationService emailRegistrationService;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredRecords() {
        try {
            emailRegistrationService.cleanupExpiredRecords();
            log.debug("Completed scheduled cleanup of expired verification records");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup: {}", e.getMessage(), e);
        }
    }
}
