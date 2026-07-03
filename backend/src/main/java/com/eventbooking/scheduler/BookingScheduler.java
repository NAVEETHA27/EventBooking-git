package com.eventbooking.scheduler;

import com.eventbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for booking lifecycle management.
 * Extracted from BookingService to follow Single Responsibility Principle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;

    /**
     * Runs every 60 seconds (configurable via booking.payment-timeout-scan-ms).
     * Releases seat reservations for bookings where payment was not completed within 10 minutes.
     */
    @Scheduled(fixedDelayString = "${booking.payment-timeout-scan-ms:60000}")
    public void releaseExpiredPaymentReservations() {
        log.debug("[Scheduler] Scanning for expired payment reservations...");
        bookingService.releaseExpiredPaymentReservations();
    }
}
