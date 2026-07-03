package com.eventbooking.scheduler;

import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for event lifecycle management.
 * Extracted from EventService to follow Single Responsibility Principle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventScheduler {

    private final EventService eventService;

    /**
     * Runs daily at 01:00 AM.
     * Marks all past events whose date has passed as COMPLETED.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void autoCompletePastEvents() {
        log.info("[Scheduler] Running autoCompletePastEvents...");
        eventService.autoCompletePastEvents();
    }
}
