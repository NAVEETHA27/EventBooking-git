package com.eventbooking.service;

import com.eventbooking.entity.*;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reusable recovery logic: generates missing certificates for completed events.
 *
 * Called from:
 * - CertificateBackfillRunner  (startup)
 * - CertificateScheduler       (daily at 02:00)
 * - CertificateController      (organizer "Generate Missing" action)
 * - CertificateController      (user "My Certificates" — triggers background recovery)
 *
 * Rules:
 * - Never generates a duplicate (existsByEventIdAndUserId guard)
 * - Continues if one booking fails (per-booking try-catch)
 * - Works for events ended before the cert module was added
 * - Does NOT bypass manual release — sets status GENERATED, not RELEASED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateRecoveryService {

    private final EventRepository               eventRepository;
    private final BookingRepository             bookingRepository;
    private final CertificateRepository         certificateRepository;
    private final CertificateSettingsRepository certificateSettingsRepository;
    private final ParticipantRepository         participantRepository;

    /**
     * Recover missing certificates for ALL completed events.
     * Called at startup and by the daily scheduler.
     */
    @Transactional
    public int recoverAll() {
        List<Event> events = eventRepository.findByStatusIn(
                        List.of(Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED))
                .stream()
                .filter(e -> e.isHasCertificate())
                .toList();

        if (events.isEmpty()) return 0;

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Booking> allBookings = bookingRepository.findConfirmedByEventIds(eventIds);

        int generated = 0;
        for (Event event : events) {
            generated += recoverForEvent(event,
                    allBookings.stream()
                            .filter(b -> b.getEvent().getId().equals(event.getId()))
                            .toList());
        }
        return generated;
    }

    /**
     * Recover missing certificates for ONE event.
     * Called by organizer "Generate Missing" action or user page trigger.
     */
    @Transactional
    public int recoverForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.isHasCertificate()) return 0;
        if (!isCompleted(event)) return 0;
        List<Booking> bookings = bookingRepository.findConfirmedWithDetailsByEventId(eventId);
        return recoverForEvent(event, bookings);
    }

    /**
     * Recover missing certificates for all events a specific user has bookings for.
     * Called when user opens "My Certificates".
     */
    @Transactional
    public int recoverForUser(Long userId) {
        int generated = 0;
        List<Booking> userBookings = bookingRepository.findByUserId(
                userId, org.springframework.data.domain.PageRequest.of(0, 100,
                        org.springframework.data.domain.Sort.by("bookedAt").descending()))
                .getContent();

        for (Booking b : userBookings) {
            Event ev = b.getEvent();
            if (ev == null || !ev.isHasCertificate() || !isCompleted(ev)) continue;
            if (certificateRepository.existsByEventIdAndUserId(ev.getId(), userId)) continue;
            try {
                generated += recoverForEvent(ev, List.of(b));
            } catch (Exception ex) {
                log.warn("[CertRecovery] Failed for user {} event {}: {}", userId, ev.getId(), ex.getMessage());
            }
        }
        return generated;
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private int recoverForEvent(Event event, List<Booking> bookings) {
        ensureSettings(event);
        int generated = 0;
        for (Booking b : bookings) {
            if (b.getUser() == null) continue;
            if (certificateRepository.existsByEventIdAndUserId(event.getId(), b.getUser().getId())) {
                log.debug("[CertRecovery] Skip event={} user={} — certificate exists",
                        event.getId(), b.getUser().getId());
                continue;
            }
            try {
                String college = null, dept = null;
                try {
                    List<Participant> parts = participantRepository.findByBookingId(b.getId());
                    if (!parts.isEmpty()) {
                        college = parts.get(0).getCollege();
                        dept    = parts.get(0).getDepartment();
                    }
                } catch (Exception ignored) {}
                if (college == null) college = b.getUser().getOrganizationName();

                String certId    = "CERT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
                String verifUrl  = "/verify/certificate/" + certId;

                certificateRepository.save(Certificate.builder()
                        .certificateId(certId)
                        .event(event)
                        .user(b.getUser())
                        .recipientName(b.getUser().getName())
                        .collegeName(college)
                        .departmentName(dept)
                        .eventName(event.getEventName())
                        .verificationToken(UUID.randomUUID().toString())
                        .verificationUrl(verifUrl)
                        .issuedAt(LocalDateTime.now())
                        .status(Certificate.CertificateStatus.GENERATED)
                        .emailSent(false)
                        .build());

                log.info("[CertRecovery] Generated CERT for event={} ({}) user={}",
                        event.getId(), event.getEventName(), b.getUser().getId());
                generated++;
            } catch (Exception ex) {
                log.warn("[CertRecovery] Failed event={} user={}: {}",
                        event.getId(), b.getUser().getId(), ex.getMessage());
            }
        }
        return generated;
    }

    private boolean isCompleted(Event e) {
        if (e.getStatus() == Event.EventStatus.COMPLETED || e.getStatus() == Event.EventStatus.EXPIRED)
            return true;
        LocalDate end = e.getEndDate() != null ? e.getEndDate() : e.getEventDate();
        return end != null && end.isBefore(LocalDate.now());
    }

    private void ensureSettings(Event event) {
        certificateSettingsRepository.findByEventId(event.getId()).ifPresentOrElse(
                s -> {
                    if (s.isMinimumAttendanceRequired()) {
                        s.setMinimumAttendanceRequired(false);
                        certificateSettingsRepository.save(s);
                    }
                },
                () -> certificateSettingsRepository.save(CertificateSettings.builder()
                        .event(event)
                        .certificateAvailable(true)
                        .minimumAttendanceRequired(false)
                        .organizerName(event.getOrganizer() != null ? event.getOrganizer().getOrganizerName() : null)
                        .organizationName(event.getOrganizer() != null ? event.getOrganizer().getOrganizationName() : null)
                        .build())
        );
    }
}
