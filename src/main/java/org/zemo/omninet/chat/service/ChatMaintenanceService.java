package org.zemo.omninet.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMaintenanceService {

    private final CallService callService;
    private final MessageService messageService;
    private final PresenceRegistry presenceRegistry;
    private final ContactService contactService;

    @Scheduled(fixedRate = 120000) // 2 minutes
    public void cleanupStaleCalls() {
        try {
            callService.cleanupStaleCalls();
        } catch (Exception e) {
            log.error("Error during scheduled call cleanup: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void clearUserCache() {
        try {
            messageService.clearUserCache();
            log.debug("User cache cleared during scheduled maintenance");
        } catch (Exception e) {
            log.error("Error during user cache cleanup: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 7200000) // 2 hours
    public void clearContactCache() {
        try {
            contactService.clearContactCache();
            log.debug("Contact cache cleared during scheduled maintenance");
        } catch (Exception e) {
            log.error("Error during contact cache cleanup: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 600000) // 10 minutes
    public void logStatistics() {
        try {
            int onlineUsers = presenceRegistry.getOnlineUserCount();
            log.info("Chat system statistics - Online users: {}", onlineUsers);
        } catch (Exception e) {
            log.error("Error during statistics logging: {}", e.getMessage(), e);
        }
    }
}
