package com.eventbooking.config;

import com.eventbooking.service.CertificateRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup hook: generates missing certificates for all completed events.
 * Delegates to CertificateRecoveryService (idempotent — never duplicates).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateBackfillRunner {

    private final CertificateRecoveryService recoveryService;

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        try {
            int n = recoveryService.recoverAll();
            if (n > 0) log.info("[CertificateBackfill] Generated {} missing certificate(s) on startup", n);
            else       log.debug("[CertificateBackfill] All certificates up to date");
        } catch (Exception ex) {
            log.warn("[CertificateBackfill] Startup recovery skipped: {}", ex.getMessage());
        }
    }
}
