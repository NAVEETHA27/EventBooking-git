package com.eventbooking.service;

import com.eventbooking.ai.AIEngine;
import com.eventbooking.entity.UserInterest;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRatingRepository;
import com.eventbooking.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Phase 2 — AI Behavioral Learning Engine.
 *
 * Tracks user interactions asynchronously and updates the behavioral profile
 * in UserInterest. Runs a weekly AI summarisation job that rewrites the
 * behavioral_summary field with a human-readable profile.
 *
 * All writes are non-blocking (@Async). Missing data → graceful no-op.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BehavioralLearningService {

    private final UserInterestRepository interestRepository;
    private final BookingRepository      bookingRepository;
    private final EventRatingRepository  ratingRepository;
    private final AIEngine               aiEngine;

    // ── Real-time event tracking ──────────────────────────────────────────

    /** Called whenever a user searches for something */
    @Async
    @Transactional
    public void recordSearch(Long userId, String query) {
        if (userId == null || query == null) return;
        interestRepository.findByUserId(userId).ifPresent(interest -> {
            interest.setTotalSearches(
                    interest.getTotalSearches() != null ? interest.getTotalSearches() + 1 : 1);
            // Append search keyword to interests if not already present
            appendKeyword(interest, query);
            interestRepository.save(interest);
            log.debug("[Behavioral] search recorded userId={}", userId);
        });
    }

    /** Called whenever a user clicks an event card or opens event detail */
    @Async
    @Transactional
    public void recordClick(Long userId, Long eventId) {
        if (userId == null) return;
        interestRepository.findByUserId(userId).ifPresent(interest -> {
            interest.setTotalClicks(
                    interest.getTotalClicks() != null ? interest.getTotalClicks() + 1 : 1);
            interestRepository.save(interest);
        });
    }

    // ── Weekly AI summarisation (every Sunday midnight) ───────────────────

    @Scheduled(cron = "0 0 0 * * SUN")
    @Transactional
    public void refreshBehavioralProfiles() {
        log.info("[Behavioral] Starting weekly profile refresh");
        interestRepository.findAll().forEach(interest -> {
            try {
                refreshProfile(interest);
            } catch (Exception ex) {
                log.warn("[Behavioral] Profile refresh failed for userId={}: {}",
                        interest.getUser() != null ? interest.getUser().getId() : "?",
                        ex.getMessage());
            }
        });
        log.info("[Behavioral] Weekly profile refresh complete");
    }

    @Async
    @Transactional
    public void refreshProfileAsync(Long userId) {
        interestRepository.findByUserId(userId).ifPresent(this::refreshProfile);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private void refreshProfile(UserInterest interest) {
        if (interest.getUser() == null) return;
        Long userId = interest.getUser().getId();

        // Build a compact behavioral summary from actual data
        var bookings = bookingRepository.findByUserId(userId,
                PageRequest.of(0, 50, Sort.by("bookedAt").descending())).getContent();
        var ratings  = ratingRepository.findByUserId(userId);

        if (bookings.isEmpty() && ratings.isEmpty()) return;

        String recentCategories = bookings.stream()
                .filter(b -> b.getEvent() != null && b.getEvent().getCategory() != null)
                .map(b -> b.getEvent().getCategory())
                .distinct().limit(5)
                .collect(Collectors.joining(", "));

        double avgRating = ratings.stream()
                .mapToInt(r -> r.getOverallRating()).average().orElse(0.0);

        if (!aiEngine.isAvailable()) {
            // Fallback: simple rule-based summary
            String summary = String.format(
                    "Attended %d events mainly in %s. Average review score given: %.1f/5. " +
                    "Department: %s. Skills: %s.",
                    bookings.size(),
                    recentCategories.isEmpty() ? "various categories" : recentCategories,
                    avgRating,
                    interest.getDepartment() != null ? interest.getDepartment() : "N/A",
                    interest.getSkills() != null ? interest.getSkills() : "N/A");
            interest.setBehavioralSummary(summary);
            interestRepository.save(interest);
            return;
        }

        String prompt = """
                Generate a concise 2-sentence behavioral profile for an event recommendation AI:
                - Events attended: %d
                - Top categories: %s
                - Average rating given: %.1f/5
                - Department: %s
                - Skills: %s
                - Career goal: %s
                Focus on patterns that help recommend future events.
                """.formatted(
                bookings.size(), recentCategories, avgRating,
                interest.getDepartment() != null ? interest.getDepartment() : "N/A",
                interest.getSkills()     != null ? interest.getSkills()     : "N/A",
                interest.getCareerGoal() != null ? interest.getCareerGoal() : "N/A");

        String summary = aiEngine.completeCached("BEHAVIORAL",
                "You are a student behavioral profile AI. Be concise.", prompt);

        if (summary != null) {
            interest.setBehavioralSummary(summary);
            interestRepository.save(interest);
            log.debug("[Behavioral] Profile updated for userId={}", userId);
        }
    }

    private void appendKeyword(UserInterest interest, String query) {
        if (query.isBlank() || query.length() > 50) return;
        String current = interest.getInterests() != null ? interest.getInterests() : "";
        String keyword = query.trim().toLowerCase();
        if (!current.toLowerCase().contains(keyword)) {
            String updated = current.isEmpty() ? keyword : current + "," + keyword;
            // Keep at most 20 keywords
            String[] parts = updated.split(",");
            if (parts.length > 20) {
                updated = String.join(",", java.util.Arrays.copyOfRange(parts, parts.length - 20, parts.length));
            }
            interest.setInterests(updated);
        }
    }
}
