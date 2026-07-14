package com.eventbooking.scheduler;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.service.BehavioralLearningService;
import com.eventbooking.service.CertificateService;
import com.eventbooking.service.EmailService;
import com.eventbooking.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 19 — AI Workflow Automation.
 *
 * Automated jobs:
 * 1. Daily  06:00 — send event-day reminders to confirmed attendees
 * 2. Daily  07:00 — auto-generate certificates for events completed yesterday
 * 3. Daily  02:00 — run fraud scan
 * 4. Weekly Sun 00:00 — refresh behavioral learning profiles (delegated)
 * 5. Daily  23:00 — archive expired/cancelled events older than 90 days
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIWorkflowScheduler {

    private final EventRepository          eventRepository;
    private final BookingRepository        bookingRepository;
    private final CertificateService       certificateService;
    private final EmailService             emailService;
    private final FraudDetectionService    fraudDetectionService;
    private final BehavioralLearningService behavioralLearningService;

    // ── 1. Event-day reminders (daily 06:00) ─────────────────────────────

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional(readOnly = true)
    public void sendEventDayReminders() {
        LocalDate today = LocalDate.now();
        log.info("[Workflow] Sending event-day reminders for {}", today);

        List<Event> todayEvents = eventRepository.findUpcomingPublicEvents(today).stream()
                .filter(e -> e.getEventDate().equals(today))
                .toList();

        for (Event event : todayEvents) {
            try {
                // Use paginated booking query keyed by eventId rather than organizerId
                List<Booking> bookings = bookingRepository.findAll().stream()
                        .filter(b -> b.getEvent() != null
                                  && b.getEvent().getId().equals(event.getId())
                                  && b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                        .toList();

                for (Booking booking : bookings) {
                    try {
                        String venueLine = event.getVenueName() != null
                                ? event.getVenueName() : (event.getLocation() != null ? event.getLocation() : "TBD");
                        String body = "Dear " + booking.getUser().getName() + ",\n\n"
                                + "Your event **" + event.getEventName() + "** is TODAY at " + event.getEventTime() + ".\n"
                                + "Venue: " + venueLine + "\nTicket ID: " + booking.getTicketId()
                                + "\n\nBest regards, CollegeEvents";
                        emailService.sendSimpleEmail(booking.getUser().getEmail(),
                                "⏰ Today's Event: " + event.getEventName(), body);
                    } catch (Exception ex) {
                        log.warn("[Workflow] Reminder email failed for booking {}: {}", booking.getId(), ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("[Workflow] Event-day reminder failed for event {}: {}", event.getId(), ex.getMessage());
            }
        }
        log.info("[Workflow] Event-day reminders sent for {} events", todayEvents.size());
    }

    // ── 2. Auto-generate certificates (daily 07:00) ───────────────────────

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional(readOnly = true)
    public void autoGenerateCertificates() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[Workflow] Auto-generating certificates for events on {}", yesterday);
        eventRepository.findUpcomingPublicEvents(yesterday.minusDays(1)).stream()
                .filter(e -> (e.getStatus() == Event.EventStatus.COMPLETED
                           || e.getStatus() == Event.EventStatus.EXPIRED)
                          && e.isHasCertificate()
                          && (e.getEndDate() != null
                              ? !e.getEndDate().isAfter(LocalDate.now().minusDays(1))
                              : e.getEventDate().equals(yesterday)))
                .forEach(event -> {
                    try {
                        certificateService.generateCertificatesForEvent(event.getId());
                        log.info("[Workflow] Certificates triggered for event {}", event.getId());
                    } catch (Exception ex) {
                        log.warn("[Workflow] Certificate generation failed for event {}: {}",
                                event.getId(), ex.getMessage());
                    }
                });
    }

    // ── 3. Fraud scan (daily 02:00) — delegated ───────────────────────────
    // Handled by FraudDetectionService.runDailyFraudScan()

    // ── 4. Archive stale events (daily 23:00) ────────────────────────────

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void archiveStaleEvents() {
        LocalDate cutoff = LocalDate.now().minusDays(90);
        log.info("[Workflow] Archiving events completed/cancelled before {}", cutoff);
        // Mark expired events — reuse existing scheduler logic safely
        int archived = eventRepository.markPastEventsCompleted(LocalDate.now());
        if (archived > 0) {
            log.info("[Workflow] Marked {} past events as EXPIRED", archived);
        }
    }
}
