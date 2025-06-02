package com.codebridge.server.scheduler;

import com.codebridge.server.sessions.SessionManager;
import com.codebridge.server.sessions.SshSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
// @EnableScheduling // Typically added to a main application class or a @Configuration class
public class SessionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupScheduler.class);
    private final SessionManager<SshSessionWrapper> sshSessionManager;

    public SessionCleanupScheduler(SessionManager<SshSessionWrapper> sshSessionManager) {
        this.sshSessionManager = sshSessionManager;
    }

    /**
     * Periodically cleans up expired SSH sessions.
     * This scheduler runs at the start of every minute.
     * Cron format: "second minute hour day-of-month month day-of-week"
     */
    @Scheduled(cron = "0 * * * * ?")
    // Alternative: @Scheduled(fixedDelay = 60000) // Runs 60 seconds after the previous one finishes
    // Alternative: @Scheduled(fixedRate = 60000) // Runs every 60 seconds, regardless of previous run time
    public void performCleanup() {
        log.info("Scheduled SSH session cleanup job starting...");
        try {
            sshSessionManager.cleanupExpiredSessions();
            log.info("Scheduled SSH session cleanup job finished.");
        } catch (Exception e) {
            log.error("Error during scheduled SSH session cleanup:", e);
        }
    }
}
