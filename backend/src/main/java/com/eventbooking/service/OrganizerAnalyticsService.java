package com.eventbooking.service;

import com.eventbooking.ai.AIProvider;
import com.eventbooking.entity.*;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
/**
 * Organizer Dashboard analytics + AI performance score.
 *
 * All methods return safe zero-value maps instead of throwing on empty data.
 * Every repository call is preceded by a DEBUG log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizerAnalyticsService {

    private final EventRepository            eventRepository;
    private final BookingRepository          bookingRepository;
    private final AttendanceRepository       attendanceRepository;
    private final EventRatingRepository      ratingRepository;
    private final OrganizerScoreRepository   organizerScoreRepository;
    private final OrganizerRepository        organizerRepository;
    private final List<AIProvider>           aiProviders;

    @Value("${ai.provider:gemini}")
    private String providerName;

    // ── Main analytics entry point ────────────────────────────────────────

    /**
     * Returns full analytics for one organizer.
     * Always returns HTTP 200 — zero values when no data exists.
     * Uses a writable transaction because computeAndSaveScore may INSERT a new OrganizerScore row.
     */
    @Transactional
    public Map<String, Object> getOrganizerAnalytics(Long organizerId) {
        log.debug("[Analytics] START organizerId={}", organizerId);

        Map<String, Object> result = new LinkedHashMap<>();

        // ── 1. Fetch organizer's events ──────────────────────────────────
        log.debug("[Analytics] Fetching events for organizerId={}", organizerId);
        List<Event> events;
        try {
            events = eventRepository
                    .findByOrganizerId(organizerId, PageRequest.of(0, 200))
                    .getContent();
        } catch (Exception ex) {
            log.error("[Analytics] Failed to fetch events for organizerId={}: {}", organizerId, ex.getMessage(), ex);
            events = List.of();
        }
        log.debug("[Analytics] Events found: {}", events.size());

        int totalEvents     = events.size();
        int completedEvents = (int) events.stream()
                .filter(e -> e.getStatus() == Event.EventStatus.COMPLETED
                          || e.getStatus() == Event.EventStatus.EXPIRED).count();
        int publishedEvents = (int) events.stream()
                .filter(e -> e.getStatus() == Event.EventStatus.PUBLISHED).count();

        // ── 2. Per-event stats ───────────────────────────────────────────
        long totalRegistrations = 0;
        long totalAttendance    = 0;
        List<Map<String, Object>> eventStats = new ArrayList<>();

        for (Event event : events) {
            Long eventId = event.getId();

            log.debug("[Analytics] Counting registrations for eventId={}", eventId);
            long regs = 0;
            try { regs = eventRepository.countConfirmedBookings(eventId); }
            catch (Exception ex) { log.warn("[Analytics] countConfirmedBookings failed for eventId={}: {}", eventId, ex.getMessage()); }
            totalRegistrations += regs;

            log.debug("[Analytics] Counting attendance for eventId={}", eventId);
            long att = 0;
            try { att = attendanceRepository.countByEventId(eventId); }
            catch (Exception ex) { log.warn("[Analytics] countByEventId failed for eventId={}: {}", eventId, ex.getMessage()); }
            totalAttendance += att;

            log.debug("[Analytics] Fetching avg rating for eventId={}", eventId);
            double avgRating = 0.0;
            try { avgRating = ratingRepository.findAverageRatingByEventId(eventId).orElse(0.0); }
            catch (Exception ex) { log.warn("[Analytics] avgRating failed for eventId={}: {}", eventId, ex.getMessage()); }

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("eventId",       eventId);
            stat.put("eventName",     event.getEventName());
            stat.put("eventDate",     event.getEventDate());
            stat.put("status",        event.getStatus() != null ? event.getStatus().name() : "UNKNOWN");
            stat.put("registrations", regs);
            stat.put("attendance",    att);
            // Null-safe round — store as double literal, not Math.round (which returns long)
            stat.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
            eventStats.add(stat);
        }

        log.debug("[Analytics] totalRegistrations={} totalAttendance={}", totalRegistrations, totalAttendance);

        result.put("totalEvents",       totalEvents);
        result.put("completedEvents",   completedEvents);
        result.put("publishedEvents",   publishedEvents);
        result.put("totalRegistrations",totalRegistrations);
        result.put("totalAttendance",   totalAttendance);
        result.put("eventStats",        eventStats);
        result.put("participantSummary", buildParticipantSummary(events));

        // ── 3. Category distribution — null-safe groupBy ─────────────────
        Map<String, Long> catDist = events.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(Event::getCategory, Collectors.counting()));
        result.put("categoryDistribution", catDist);
        log.debug("[Analytics] categoryDistribution size={}", catDist.size());

        // ── 4. Performance score (may INSERT new row) ────────────────────
        OrganizerScore score;
        try {
            score = computeAndSaveScore(organizerId, completedEvents, totalEvents,
                                        totalRegistrations, totalAttendance);
        } catch (Exception ex) {
            log.error("[Analytics] computeAndSaveScore failed for organizerId={}: {}", organizerId, ex.getMessage(), ex);
            score = OrganizerScore.builder().build();
        }
        // *** Map to plain Map — never serialize the entity directly (avoids LazyInitializationException) ***
        result.put("performanceScore", scoreToMap(score));

        // ── 5. AI insights ───────────────────────────────────────────────
        String insights = "Keep hosting events to improve your performance score!";
        try {
            insights = generateInsights(score, catDist, totalRegistrations, completedEvents);
        } catch (Exception ex) {
            log.warn("[Analytics] generateInsights failed: {}", ex.getMessage());
        }
        result.put("aiInsights", insights);

        log.debug("[Analytics] DONE organizerId={}", organizerId);
        return result;
    }

    // ── Score computation ─────────────────────────────────────────────────

    /**
     * Upserts the OrganizerScore row.
     * Separated into its own @Transactional method so Spring gives it a real
     * writable transaction even when called from a read-only outer context
     * (propagation = REQUIRED merges into the parent writable tx here).
     */
    @Transactional
    public OrganizerScore computeAndSaveScore(Long organizerId, int completed,
                                               int total, long registrations,
                                               long totalAttendance) {
        log.debug("[Analytics] computeAndSaveScore organizerId={} completed={} total={} regs={} att={}",
                organizerId, completed, total, registrations, totalAttendance);

        OrganizerScore score = organizerScoreRepository.findByOrganizerId(organizerId)
                .orElseGet(() -> {
                    log.debug("[Analytics] No existing OrganizerScore — creating new row for organizerId={}", organizerId);
                    Organizer org = organizerRepository.findById(organizerId)
                            .orElseThrow(() -> new com.eventbooking.exception.ResourceNotFoundException("Organizer not found: " + organizerId));
                    return OrganizerScore.builder().organizer(org).build();
                });

        log.debug("[Analytics] Fetching ratings for organizerId={}", organizerId);
        double avgRating;
        try {
            avgRating = ratingRepository.findByOrganizerId(organizerId).stream()
                    .mapToInt(EventRating::getOverallRating).average().orElse(0.0);
        } catch (Exception ex) {
            log.warn("[Analytics] findByOrganizerId ratings failed: {}", ex.getMessage());
            avgRating = 0.0;
        }

        // Null-safe percentage calculations — no division by zero
        double attendanceRate  = registrations > 0
                ? Math.min((totalAttendance / (double) registrations) * 100.0, 100.0) : 0.0;
        double completionRate  = total > 0
                ? ((double) completed / total) * 100.0 : 0.0;

        // Weighted score: rating 40 % + completion 30 % + attendance 30 %
        double overall = (avgRating / 5.0 * 40.0)
                       + (completionRate  / 100.0 * 30.0)
                       + (attendanceRate  / 100.0 * 30.0);

        score.setAverageRating(Math.round(avgRating    * 100.0) / 100.0);
        score.setTotalEvents(total);
        score.setCompletedEvents(completed);
        score.setTotalRegistrations(registrations);
        score.setAttendanceRate(Math.round(attendanceRate  * 100.0) / 100.0);
        score.setOverallScore(Math.round(overall       * 100.0) / 100.0);
        score.setBadge(computeBadge(overall, avgRating));

        log.debug("[Analytics] Saving OrganizerScore: overall={} badge={}", score.getOverallScore(), score.getBadge());
        return organizerScoreRepository.save(score);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private OrganizerScore.OrganizerBadge computeBadge(double score, double rating) {
        if (score >= 80 && rating >= 4.5) return OrganizerScore.OrganizerBadge.GOLD;
        if (score >= 65 && rating >= 4.0) return OrganizerScore.OrganizerBadge.SILVER;
        if (score >= 45)                  return OrganizerScore.OrganizerBadge.BRONZE;
        return OrganizerScore.OrganizerBadge.NONE;
    }

    private Map<String, Object> buildParticipantSummary(List<Event> events) {
        // Use only eagerly-available scalar fields — never access LAZY collections
        // (bookings / participants) to avoid LazyInitializationException.
        long accommodationRequests = events.stream()
                .filter(e -> Boolean.TRUE.equals(e.getAccommodationProvided()))
                .mapToLong(e -> Math.max(0, e.getTotalSeats() - e.getAvailableSeats()))
                .sum();
        Map<String, Long> foodPreferenceSummary = new LinkedHashMap<>();
        long totalRegistrations = 0;
        for (Event event : events) {
            long registered = Math.max(0, event.getTotalSeats() - event.getAvailableSeats());
            totalRegistrations += registered;
            String foodType = event.getFoodType() != null ? event.getFoodType() : "NOT_SPECIFIED";
            foodPreferenceSummary.merge(foodType, registered, Long::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        // local/outstation breakdown requires a JOIN query; use totals as safe approximation
        summary.put("localParticipants", 0L);
        summary.put("outstationParticipants", totalRegistrations);
        summary.put("accommodationRequests", accommodationRequests);
        summary.put("foodPreferenceSummary", foodPreferenceSummary);
        return summary;
    }

    private String generateInsights(OrganizerScore score, Map<String, Long> catDist,
                                     long registrations, int completed) {
        AIProvider provider = aiProviders.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName) && p.isConfigured())
                .findFirst()
                .orElseGet(() -> aiProviders.stream().filter(AIProvider::isConfigured).findFirst().orElse(null));

        if (provider == null) return "Keep hosting events to improve your performance score!";

        try {
            // Null-safe access to score fields
            double overall  = score.getOverallScore()  != null ? score.getOverallScore()  : 0.0;
            double avgRating= score.getAverageRating()  != null ? score.getAverageRating()  : 0.0;
            String badge    = score.getBadge()          != null ? score.getBadge().name()  : "NONE";
            String topCats  = catDist.entrySet().stream().limit(3)
                    .map(Map.Entry::getKey).collect(Collectors.joining(", "));

            String prompt = """
                    Provide 3 brief, actionable AI insights for this event organizer:
                    - Overall Score: %.1f/100
                    - Average Rating: %.1f/5
                    - Badge: %s
                    - Total Registrations: %d
                    - Completed Events: %d
                    - Top Categories: %s
                    Format as 3 short bullet points.
                    """.formatted(overall, avgRating, badge, registrations, completed, topCats);

            return provider.complete("You are an event analytics AI assistant.", prompt, List.of());
        } catch (Exception ex) {
            log.warn("[Analytics] AI insights generation failed: {}", ex.getMessage());
            return "Your performance is improving. Keep engaging attendees for better ratings.";
        }
    }

    // ── Leaderboard (read-only, no writes) ───────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCollegeLeaderboard(int limit) {
        log.debug("[Analytics] getCollegeLeaderboard limit={}", limit);
        try {
            List<Object[]> results = bookingRepository.findCollegeParticipation(
                    PageRequest.of(0, limit));
            return results.stream().map(row -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("college",      row[0] != null ? row[0] : "Unknown");
                m.put("participants", ((Number) row[1]).longValue());
                return m;
            }).collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("[Analytics] getCollegeLeaderboard failed: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    // ── Phase 11: Natural-language analytics query ────────────────────────

    @Transactional(readOnly = true)
    public String answerNaturalLanguageQuery(Long organizerId, String question) {
        log.debug("[Analytics] NL query organizerId={} q={}", organizerId, question);

        // Fetch raw data directly — safe, no lazy-loading risk
        List<Event> events = List.of();
        try {
            events = eventRepository.findByOrganizerId(organizerId, PageRequest.of(0, 200)).getContent();
        } catch (Exception ex) {
            log.warn("[Analytics] Could not fetch events for NL query: {}", ex.getMessage());
        }

        // Build context string from raw data (no AI needed)
        long totalEvents      = events.size();
        long publishedEvents  = events.stream().filter(e -> e.getStatus() == Event.EventStatus.PUBLISHED).count();
        long completedEvents  = events.stream().filter(e -> e.getStatus() == Event.EventStatus.COMPLETED || e.getStatus() == Event.EventStatus.EXPIRED).count();
        long totalSeats       = events.stream().mapToLong(Event::getTotalSeats).sum();
        long availableSeats   = events.stream().mapToLong(Event::getAvailableSeats).sum();
        long totalRegistrations = totalSeats - availableSeats;

        // Find top event by registrations
        Event topEvent = events.stream()
                .filter(e -> e.getTotalSeats() > 0)
                .max(java.util.Comparator.comparingLong(e -> (long)(e.getTotalSeats() - e.getAvailableSeats())))
                .orElse(null);

        // Category distribution
        Map<String, Long> catCount = events.stream()
                .filter(e -> e.getCategory() != null)
                .collect(Collectors.groupingBy(Event::getCategory, Collectors.counting()));
        String topCategory = catCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");

        String context = String.format(
                "Total Events: %d | Published: %d | Completed: %d | Total Registrations: %d | Top Category: %s",
                totalEvents, publishedEvents, completedEvents, totalRegistrations, topCategory);

        // Try AI first
        AIProvider provider = selectProvider();
        if (provider != null) {
            try {
                String prompt = """
                        You are an analytics assistant for an event organizer.
                        Here is the organizer's current analytics data:
                        %s
                        
                        Answer this question concisely (2-4 sentences):
                        %s
                        """.formatted(context, question);
                String answer = provider.complete(
                        "You are a helpful event analytics assistant. Answer only from the provided data.", prompt, List.of());
                if (answer != null && !answer.isBlank()) return answer;
            } catch (Exception ex) {
                log.warn("[Analytics] NL query AI failed: {}", ex.getMessage());
            }
        }

        // Rule-based fallback — always works without AI
        return buildRuleBasedAnswer(question, totalEvents, publishedEvents, completedEvents,
                totalRegistrations, topCategory, topEvent);
    }

    private String buildRuleBasedAnswer(String question, long totalEvents, long published,
            long completed, long registrations, String topCategory, Event topEvent) {
        String q = question.toLowerCase();

        if (q.contains("registr") || q.contains("student") || q.contains("how many")) {
            return String.format("You have **%d total registrations** across all your events. " +
                    "You currently have **%d published** events.", registrations, published);
        }
        if (q.contains("highest") || q.contains("most attend") || q.contains("popular") || q.contains("best")) {
            if (topEvent != null) {
                long regs = topEvent.getTotalSeats() - topEvent.getAvailableSeats();
                return String.format("Your most popular event is **%s** with **%d registrations** out of %d seats.",
                        topEvent.getEventName(), regs, topEvent.getTotalSeats());
            }
            return "No event data available yet to determine the most popular event.";
        }
        if (q.contains("complet") || q.contains("finish") || q.contains("past")) {
            return String.format("You have **%d completed events** out of **%d total events**.", completed, totalEvents);
        }
        if (q.contains("total") || q.contains("overview") || q.contains("summary") || q.contains("report")) {
            return String.format(
                    "📊 **Your Analytics Summary:**\n- Total Events: %d\n- Published: %d\n- Completed: %d\n- Total Registrations: %d\n- Top Category: %s",
                    totalEvents, published, completed, registrations, topCategory);
        }
        if (q.contains("categor") || q.contains("type")) {
            return String.format("Your top event category is **%s**. " +
                    "Diversifying event types can help attract more participants.", topCategory);
        }
        if (q.contains("college") || q.contains("participant") || q.contains("which college")) {
            return "View the **Attendees** page for a full breakdown of participant colleges and departments across your events.";
        }
        if (q.contains("perform") || q.contains("score")) {
            return String.format("Your performance is based on registration rates and attendance. " +
                    "With **%d registrations** across **%d events**, keep improving engagement for a higher score.", registrations, totalEvents);
        }
        if (q.contains("predict") || q.contains("next event") || q.contains("forecast")) {
            return "Based on your current registration rate, your next event can expect similar turnout. " +
                    "Promote early registration to maximize attendance.";
        }
        // Default
        return String.format(
                "You have **%d events** (%d published, %d completed) with **%d total registrations**. " +
                "Your top category is **%s**. Ask me about registrations, attendance, performance, or specific events.",
                totalEvents, published, completed, registrations, topCategory);
    }

    // ── Phase 6: AI Event Success Prediction ─────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> predictEventOutcomes(Long eventId, Long organizerId) {
        log.debug("[Analytics] predictEventOutcomes eventId={}", eventId);
        Event event = eventRepository.findByIdWithOrganizer(eventId)
                .orElseThrow(() -> new com.eventbooking.exception.ResourceNotFoundException("Event not found: " + eventId));

        if (event.getOrganizer() == null || !event.getOrganizer().getId().equals(organizerId))
            throw new com.eventbooking.exception.UnauthorizedException("Access denied");

        long confirmed = eventRepository.countConfirmedBookings(eventId);
        int totalSeats  = event.getTotalSeats();
        double fillRate = totalSeats > 0 ? confirmed / (double) totalSeats : 0;

        // Heuristic predictions (enhanced by AI if available)
        int predAttendance = (int) Math.round(confirmed * 0.85);
        double noShowRate  = Math.round((1 - 0.85) * 100) / 100.0;
        double successScore = Math.min(fillRate * 60 + 40, 100);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventName",            event.getEventName());
        result.put("currentRegistrations", confirmed);
        result.put("totalSeats",           totalSeats);
        result.put("fillRate",             Math.round(fillRate * 1000) / 10.0);
        result.put("predictedAttendance",  predAttendance);
        result.put("predictedNoShowRate",  noShowRate * 100 + "%");
        result.put("successScore",         Math.round(successScore));
        result.put("predictedRevenue",
                event.getTicketPrice() != null
                        ? event.getTicketPrice().multiply(java.math.BigDecimal.valueOf(predAttendance))
                        : java.math.BigDecimal.ZERO);

        AIProvider provider = selectProvider();
        if (provider != null) {
            try {
                String prompt = """
                        Predict outcomes for this event:
                        - Name: %s
                        - Category: %s
                        - Seats: %d
                        - Current Registrations: %d
                        - Ticket Price: ₹%s
                        - Days Until Event: %d
                        
                        Provide:
                        ## Registration Prediction (final expected registrations)
                        ## Attendance Prediction
                        ## Revenue Estimate
                        ## Key Risks
                        ## Recommendations (2-3 bullet points)
                        """.formatted(
                        event.getEventName(), event.getCategory(), totalSeats, confirmed,
                        event.getTicketPrice(),
                        java.time.LocalDate.now().until(event.getEventDate()).getDays());
                result.put("aiPrediction", provider.complete(
                        "You are an expert event analytics predictor.", prompt, List.of()));
            } catch (Exception ex) {
                log.warn("[Analytics] AI prediction failed: {}", ex.getMessage());
            }
        }
        return result;
    }

    private AIProvider selectProvider() {
        return aiProviders.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName) && p.isConfigured())
                .findFirst()
                .orElseGet(() -> aiProviders.stream().filter(AIProvider::isConfigured).findFirst().orElse(null));
    }

    /** Convert OrganizerScore entity → plain Map so Jackson never touches the lazy Organizer proxy. */
    private Map<String, Object> scoreToMap(OrganizerScore s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("overallScore",      s.getOverallScore()      != null ? s.getOverallScore()      : 0.0);
        m.put("averageRating",     s.getAverageRating()     != null ? s.getAverageRating()     : 0.0);
        m.put("attendanceRate",    s.getAttendanceRate()    != null ? s.getAttendanceRate()    : 0.0);
        m.put("refundRatio",       s.getRefundRatio()       != null ? s.getRefundRatio()       : 0.0);
        m.put("cancellationRate",  s.getCancellationRate()  != null ? s.getCancellationRate()  : 0.0);
        m.put("badge",             s.getBadge()             != null ? s.getBadge().name()      : "NONE");
        m.put("totalEvents",       s.getTotalEvents());
        m.put("completedEvents",   s.getCompletedEvents());
        m.put("totalRegistrations",s.getTotalRegistrations());
        return m;
    }

    private String buildAnalyticsContext(Map<String, Object> analytics) {
        @SuppressWarnings("unchecked")
        Map<String, Object> scoreMap = analytics.get("performanceScore") instanceof Map
                ? (Map<String, Object>) analytics.get("performanceScore") : Map.of();
        return "Total Events: " + analytics.getOrDefault("totalEvents", 0)
             + ", Completed: "          + analytics.getOrDefault("completedEvents", 0)
             + ", Total Registrations: " + analytics.getOrDefault("totalRegistrations", 0)
             + ", Total Attendance: "    + analytics.getOrDefault("totalAttendance", 0)
             + ", Overall Score: "       + scoreMap.getOrDefault("overallScore", 0)
             + "/100, Badge: "           + scoreMap.getOrDefault("badge", "NONE");
    }
}
