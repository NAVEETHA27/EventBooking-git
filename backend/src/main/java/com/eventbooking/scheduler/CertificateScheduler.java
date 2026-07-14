package com.eventbooking.scheduler;

import com.eventbooking.service.CertificateRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduled job: recovers missing certificates for all completed events.
 * Runs at 02:00 AM every day.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateScheduler {

    private final CertificateRecoveryService recoveryService;

    @Scheduled(cron = "0 0 2 * * *")
    public void generatePendingCertificates() {
        log.info("[CertificateScheduler] Starting certificate recovery run...");
        try {
            int n = recoveryService.recoverAll();
            log.info("[CertificateScheduler] Recovery complete — {} certificate(s) generated", n);
        } catch (Exception ex) {
            log.error("[CertificateScheduler] Recovery failed: {}", ex.getMessage(), ex);
        }
    }
}
