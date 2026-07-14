package com.eventbooking.scheduler;

import com.eventbooking.entity.ApprovalRequest;
import com.eventbooking.entity.Event;
import com.eventbooking.repository.ApprovalRequestRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for event lifecycle management.
 *
 * Lifecycle transitions:
 *  PUBLISHED → UPCOMING  : when eventDate > today (already published, just cosmetic status)
 *  PUBLISHED/UPCOMING → LIVE      : when eventDate <= today <= endDate
 *  LIVE/UPCOMING/PUBLISHED → COMPLETED : when endDate (or eventDate) has passed
 *  COMPLETED → EXPIRED   : when certificateDeadline (default: eventDate+7) has passed
 *
 * <p><strong>Transaction design:</strong> Each individual event/approval operation
 * is delegated to {@link EventSchedulerTransactionHelper}, which runs in its own
 * {@code REQUIRES_NEW} transaction. This avoids the Spring AOP self-invocation
 * problem that previously caused {@code LazyInitializationException} when
 * {@code @Transactional} methods were called within the same class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventScheduler {

    private final EventService                       eventService;
    private final ApprovalRequestRepository          approvalRequestRepository;
    private final EventRepository                    eventRepository;
    private final EventSchedulerTransactionHelper    txHelper;

    // ── 1. Auto-publish pending approvals (every 60 s) ────────────────────

    @Scheduled(fixedDelay = 60_000)
    public void autoApproveAndPublishEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        // Pass 1: approval-request-driven — each approval in its own transaction
        List<ApprovalRequest> pendingApprovals = approvalRequestRepository.findPendingOlderThan(cutoff);
        log.debug("[EventScheduler] Pass1: found {} pending approvals older than {}", pendingApprovals.size(), cutoff);
        for (ApprovalRequest approval : pendingApprovals) {
            try {
                txHelper.publishOneApproval(approval.getId());
            } catch (Exception ex) {
                log.error("[EventScheduler] Pass1 failed for approval {}: {}", approval.getId(), ex.getMessage(), ex);
            }
        }

        // Pass 2: safety net — each stuck event in its own transaction
        List<Event> stuckEvents = eventRepository.findByStatusInAndCreatedAtBefore(
                List.of(Event.EventStatus.PENDING_APPROVAL, Event.EventStatus.APPROVED), cutoff);
        log.debug("[EventScheduler] Pass2: found {} stuck events before {}", stuckEvents.size(), cutoff);
        for (Event event : stuckEvents) {
            try {
                txHelper.publishOneStuckEvent(event.getId());
            } catch (Exception ex) {
                log.error("[EventScheduler] Pass2 failed for event {}: {}", event.getId(), ex.getMessage(), ex);
            }
        }
    }

    // ── 2. Full lifecycle transitions (every hour) ────────────────────────

    @Scheduled(cron = "0 0 * * * *")
    public void runLifecycleTransitions() {
        LocalDate today = LocalDate.now();
        int liveCount = 0, completedCount = 0, expiredCount = 0;

        for (Event e : eventRepository.findByStatusInAndEventDateLessThanEqual(
                List.of(Event.EventStatus.PUBLISHED, Event.EventStatus.UPCOMING), today)) {
            try { if (txHelper.transitionToLive(e.getId(), today)) liveCount++; }
            catch (Exception ex) { log.error("[Lifecycle] LIVE failed event {}: {}", e.getId(), ex.getMessage(), ex); }
        }

        for (Event e : eventRepository.findByStatusInAndEndDateBefore(
                List.of(Event.EventStatus.LIVE, Event.EventStatus.PUBLISHED,
                        Event.EventStatus.UPCOMING, Event.EventStatus.ONGOING), today)) {
            try { if (txHelper.transitionToCompleted(e.getId(), today)) completedCount++; }
            catch (Exception ex) { log.error("[Lifecycle] COMPLETED failed event {}: {}", e.getId(), ex.getMessage(), ex); }
        }

        for (Event e : eventRepository.findByStatusInAndEventDateBeforeAndEndDateIsNull(
                List.of(Event.EventStatus.LIVE, Event.EventStatus.PUBLISHED,
                        Event.EventStatus.UPCOMING, Event.EventStatus.ONGOING), today)) {
            try { if (txHelper.transitionToCompleted(e.getId(), today)) completedCount++; }
            catch (Exception ex) { log.error("[Lifecycle] COMPLETED(no-end) failed event {}: {}", e.getId(), ex.getMessage(), ex); }
        }

        for (Event e : eventRepository.findCompletedPastCertDeadline(today)) {
            try { if (txHelper.transitionToExpired(e.getId())) expiredCount++; }
            catch (Exception ex) { log.error("[Lifecycle] EXPIRED failed event {}: {}", e.getId(), ex.getMessage(), ex); }
        }

        for (Event e : eventRepository.findPublishedPastRegistrationDeadline(today)) {
            try { if (txHelper.transitionToExpired(e.getId())) expiredCount++; }
            catch (Exception ex) { log.error("[Lifecycle] REG_DEADLINE expire failed event {}: {}", e.getId(), ex.getMessage(), ex); }
        }

        if (liveCount + completedCount + expiredCount > 0) {
            log.info("[Lifecycle] {} → LIVE, {} → COMPLETED, {} → EXPIRED", liveCount, completedCount, expiredCount);
        }
    }

    // ── 3. Daily cleanup at 01:00 AM ──────────────────────────────────────

    @Scheduled(cron = "0 0 1 * * *")
    public void autoCompletePastEvents() {
        log.info("[EventScheduler] Running autoCompletePastEvents...");
        eventService.autoCompletePastEvents();
    }
}
