package com.eventbooking.service;

import com.eventbooking.ai.AIProvider;
import com.eventbooking.entity.*;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered event recommendation engine.
 * Generates personalised recommendations based on user interests,
 * booking history, attendance, certificates, ratings and location.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final CertificateRepository certificateRepository;
    private final EventRatingRepository ratingRepository;
    private final UserInterestRepository interestRepository;
    private final EventRecommendationRepository recommendationRepository;
    private final com.eventbooking.ai.AIEngine aiEngine;

    // Category keys used across discover page
    public static final String RECOMMENDED_FOR_YOU  = "RECOMMENDED_FOR_YOU";
    public static final String AI_PICKS             = "AI_PICKS";
    public static final String TRENDING             = "TRENDING";
    public static final String NEAR_YOU             = "NEAR_YOU";
    public static final String POPULAR              = "POPULAR";
    public static final String UPCOMING_THIS_WEEK   = "UPCOMING_THIS_WEEK";
    public static final String POPULAR_IN_DEPARTMENT= "POPULAR_IN_DEPARTMENT";
    public static final String POPULAR_IN_COLLEGE   = "POPULAR_IN_COLLEGE";
    public static final String RECENTLY_ADDED       = "RECENTLY_ADDED";
    public static final String HIGHEST_RATED        = "HIGHEST_RATED";
    public static final String FREE_EVENTS          = "FREE_EVENTS";
    public static final String ALL_LIVE_EVENTS      = "ALL_LIVE_EVENTS";

    /**
     * Returns all recommendation categories for the discover page.
     * Results are freshly computed or served from cache (expires in 6 hours).
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> getDiscoverRecommendations(Long userId,
                                                                              Double userLat, Double userLon) {
        log.debug("[Discover] Starting — userId={}, lat={}, lon={}", userId, userLat, userLon);

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        List<Event> upcoming = eventRepository.findUpcomingPublicEvents(LocalDate.now());
        log.debug("[Discover] Upcoming events fetched: count={}", upcoming.size());
        if (upcoming.isEmpty()) return result;

        result.put(ALL_LIVE_EVENTS, scoredList(upcoming.stream().limit(48).toList(), "Open for registration"));

        // 1. Trending — highest registrations in last 7 days
        result.put(TRENDING, scoredList(trendingEvents(upcoming), null));

        // 2. Upcoming this week
        LocalDate weekEnd = LocalDate.now().plusDays(7);
        List<Event> thisWeek = upcoming.stream()
                .filter(e -> !e.getEventDate().isAfter(weekEnd))
                .limit(8).toList();
        result.put(UPCOMING_THIS_WEEK, scoredList(thisWeek, null));

        // 3. Highest rated
        result.put(HIGHEST_RATED, highestRatedEvents(upcoming));

        // 4. Recently added — newest first, higher limit so just-published events always appear
        List<Event> recent = upcoming.stream()
                .sorted(Comparator.comparing(Event::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12).toList();
        result.put(RECENTLY_ADDED, scoredList(recent, null));

        // 5. Free events
        List<Event> freeEvents = upcoming.stream()
                .filter(e -> e.getTicketPrice() != null &&
                             e.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) == 0)
                .limit(8).toList();
        result.put(FREE_EVENTS, scoredList(freeEvents, null));

        // User-specific (requires login)
        if (userId != null) {
            log.debug("[Discover] User-specific section — userId={}", userId);
            UserInterest interest = interestRepository.findByUserId(userId).orElse(null);
            log.debug("[Discover] UserInterest loaded: hasInterest={}", interest != null);

            Set<Long> alreadyBooked = bookingRepository.findByUserId(userId,
                    org.springframework.data.domain.PageRequest.of(0, 100,
                            org.springframework.data.domain.Sort.by("bookedAt").descending()))
                    .stream().map(b -> b.getEvent().getId()).collect(Collectors.toSet());

            List<Event> candidates = upcoming.stream()
                    .filter(e -> !alreadyBooked.contains(e.getId()))
                    .collect(Collectors.toList());
            log.debug("[Discover] Candidate events (unbooked): count={}", candidates.size());

            // 6. Recommended For You
            result.put(RECOMMENDED_FOR_YOU, recommendForUser(candidates, interest, alreadyBooked));

            // 7. AI Picks (AI-generated with reasoning)
            result.put(AI_PICKS, aiPicksForUser(candidates, interest, userId));

            // 8. Popular in department
            if (interest != null && interest.getDepartment() != null) {
                List<Event> deptEvents = candidates.stream()
                        .filter(e -> matchesDept(e, interest.getDepartment()))
                        .limit(8).toList();
                result.put(POPULAR_IN_DEPARTMENT, scoredList(deptEvents, "Matches your department: " + interest.getDepartment()));
            }

            // 9. Popular in college
            if (interest != null && interest.getCollege() != null) {
                List<Event> collegeEvents = candidates.stream()
                        .filter(e -> e.getCollegeName() != null &&
                                e.getCollegeName().equalsIgnoreCase(interest.getCollege()))
                        .limit(8).toList();
                result.put(POPULAR_IN_COLLEGE, scoredList(collegeEvents, "From your college"));
            }

            // 10. Near you (if location provided)
            if (userLat != null && userLon != null) {
                log.debug("[Discover] Location provided: lat={}, lon={}", userLat, userLon);
                result.put(NEAR_YOU, nearbyEvents(upcoming, userLat, userLon));
            }
        }

        // General popular (by available seat fill ratio)
        result.put(POPULAR, popularEvents(upcoming));

        log.debug("[Discover] Complete — categories returned: {}", result.keySet());
        return result;
    }

    private List<Map<String, Object>> recommendForUser(List<Event> candidates,
                                                        UserInterest interest,
                                                        Set<Long> alreadyBooked) {
        if (candidates.isEmpty()) return List.of();
        return candidates.stream().map(e -> {
            double score = computeMatchScore(e, interest);
            Map<String, Object> m = eventMap(e);
            // Store as int to avoid ClassCastException (Math.round returns long)
            m.put("matchScore", (int) Math.round(score));
            m.put("reason", buildReason(e, interest));
            return m;
        }).sorted((a, b) -> Integer.compare(
                ((Number) b.get("matchScore")).intValue(),
                ((Number) a.get("matchScore")).intValue()))
                .limit(8).collect(Collectors.toList());
    }

    private List<Map<String, Object>> aiPicksForUser(List<Event> candidates,
                                                      UserInterest interest, Long userId) {
        // Sort candidates locally by score first
        List<Event> top = candidates.stream()
                .sorted(Comparator.comparingDouble((Event e) -> computeMatchScore(e, interest)).reversed())
                .limit(3).collect(Collectors.toList()); // limit to 3 to keep prompt tiny and save API cost

        if (top.isEmpty()) return List.of();

        List<Map<String, Object>> picks = new ArrayList<>();
        for (Event e : top) {
            Map<String, Object> m = eventMap(e);
            double score = computeMatchScore(e, interest);
            m.put("matchScore", (int) Math.round(score));

            String reason = "AI Pick: " + buildReason(e, interest);
            if (aiEngine.isAvailable() && interest != null) {
                try {
                    String prompt = String.format(
                        "Student interests: Favorite categories: %s, Skills: %s, Career goal: %s. " +
                        "Event details: Title: %s, Category: %s, Skills: %s. " +
                        "Explain in max 15 words why this is a good match.",
                        interest.getFavoriteCategories(), interest.getSkills(), interest.getCareerGoal(),
                        e.getEventName(), e.getCategory(), e.getTags()
                    );
                    String explanation = aiEngine.completeCached("RECOMMENDATIONS",
                        "You are an event matchmaking AI. Write a one-sentence explanation of why this event matches the student's interest profile. Do not use generic phrases. Max 15 words.",
                        prompt
                    );
                    if (explanation != null && !explanation.isBlank()) {
                        reason = "AI Pick: " + explanation.trim();
                    }
                } catch (Exception ex) {
                    log.warn("[RecommendationService] Failed to generate AI pick reasoning: {}", ex.getMessage());
                }
            }
            m.put("reason", reason);
            picks.add(m);
        }
        return picks;
    }

    private List<Map<String, Object>> highestRatedEvents(List<Event> events) {
        return events.stream().map(e -> {
            double avg = ratingRepository.findAverageRatingByEventId(e.getId()).orElse(0.0);
            long count = ratingRepository.countApprovedByEventId(e.getId());
            Map<String, Object> m = eventMap(e);
            m.put("averageRating", avg);
            m.put("ratingCount", count);
            m.put("matchScore", (int) Math.round(avg * 20)); // 5-star → 100
            return m;
        }).filter(m -> ((Number) m.get("averageRating")).doubleValue() > 0)
          .sorted((a, b) -> Double.compare(
              ((Number) b.get("averageRating")).doubleValue(),
              ((Number) a.get("averageRating")).doubleValue()))
          .limit(8).collect(Collectors.toList());
    }

    private List<Event> trendingEvents(List<Event> events) {
        return events.stream()
                .sorted(Comparator.comparingInt((Event e) ->
                        e.getTotalSeats() - e.getAvailableSeats()).reversed())
                .limit(8).collect(Collectors.toList());
    }

    private List<Map<String, Object>> popularEvents(List<Event> events) {
        return events.stream().map(e -> {
            double fillPct = e.getTotalSeats() > 0
                    ? ((double)(e.getTotalSeats() - e.getAvailableSeats()) / e.getTotalSeats()) * 100
                    : 0;
            Map<String, Object> m = eventMap(e);
            m.put("matchScore", (int) Math.round(fillPct));
            m.put("reason", String.format("%.0f%% seats filled", fillPct));
            return m;
        }).filter(m -> ((Number) m.get("matchScore")).intValue() > 10)
          .sorted((a, b) -> Integer.compare(
              ((Number) b.get("matchScore")).intValue(),
              ((Number) a.get("matchScore")).intValue()))
          .limit(8).collect(Collectors.toList());
    }

    private List<Map<String, Object>> nearbyEvents(List<Event> events, double lat, double lon) {
        return events.stream()
                .filter(e -> e.getEventType() != null && !e.getEventType().equalsIgnoreCase("ONLINE"))
                .map(e -> {
                    // Use location string as proxy — real distance needs lat/lon on event
                    Map<String, Object> m = eventMap(e);
                    m.put("matchScore", 75);
                    m.put("reason", "Near your location");
                    return m;
                })
                .limit(8).collect(Collectors.toList());
    }

    private List<Map<String, Object>> scoredList(List<Event> events, String reason) {
        return events.stream().map(e -> {
            Map<String, Object> m = eventMap(e);
            m.put("matchScore", 70);
            m.put("reason", reason != null ? reason : "");
            return m;
        }).collect(Collectors.toList());
    }

    private double computeMatchScore(Event event, UserInterest interest) {
        if (interest == null) return 50.0;
        double score = 0;
        // Department match: 25 pts
        if (interest.getDepartment() != null && matchesDept(event, interest.getDepartment())) score += 25;
        // Category match from favorites: 30 pts
        if (interest.getFavoriteCategories() != null) {
            String[] favs = interest.getFavoriteCategories().split(",");
            for (String fav : favs) {
                if (event.getCategory() != null && event.getCategory().equalsIgnoreCase(fav.trim())) {
                    score += 30; break;
                }
            }
        }
        // Skills match: 20 pts
        if (interest.getSkills() != null && event.getTags() != null) {
            String[] skills = interest.getSkills().toLowerCase().split(",");
            String tags = event.getTags().toLowerCase();
            for (String skill : skills) {
                if (tags.contains(skill.trim())) { score += 20; break; }
            }
        }
        // College match: 15 pts
        if (interest.getCollege() != null && event.getCollegeName() != null &&
                event.getCollegeName().equalsIgnoreCase(interest.getCollege())) {
            score += 15;
        }
        // Type preference: 10 pts
        if (interest.getPreferredEventType() != null && event.getEventType() != null &&
                event.getEventType().equalsIgnoreCase(interest.getPreferredEventType())) {
            score += 10;
        }
        // Recency boost: up to 5 pts — events starting within 7 days rank higher
        long daysUntil = java.time.LocalDate.now().until(event.getEventDate()).getDays();
        if (daysUntil >= 0 && daysUntil <= 7) score += 5;
        else if (daysUntil <= 14) score += 3;
        // Certificate preference boost: 5 pts
        if (event.isHasCertificate() && interest.getCareerGoal() != null) score += 5;
        // Popularity signal: up to 5 pts (% seats filled)
        if (event.getTotalSeats() > 0) {
            double fillRate = (double)(event.getTotalSeats() - event.getAvailableSeats()) / event.getTotalSeats();
            score += fillRate * 5;
        }
        return Math.min(score, 100);
    }

    private String buildReason(Event event, UserInterest interest) {
        if (interest == null) return "Popular on platform";
        List<String> reasons = new ArrayList<>();
        if (interest.getDepartment() != null && matchesDept(event, interest.getDepartment()))
            reasons.add("Matches your department");
        if (interest.getFavoriteCategories() != null) {
            for (String fav : interest.getFavoriteCategories().split(",")) {
                if (event.getCategory() != null && event.getCategory().equalsIgnoreCase(fav.trim())) {
                    reasons.add("Matches your favorite category: " + event.getCategory());
                    break;
                }
            }
        }
        if (reasons.isEmpty()) return "Recommended based on your profile";
        return String.join(" · ", reasons);
    }

    private boolean matchesDept(Event event, String dept) {
        return (event.getDepartmentName() != null &&
                event.getDepartmentName().toLowerCase().contains(dept.toLowerCase())) ||
               (event.getTags() != null &&
                event.getTags().toLowerCase().contains(dept.toLowerCase())) ||
               (event.getCategory() != null &&
                event.getCategory().toLowerCase().contains(dept.toLowerCase().substring(0, Math.min(4, dept.length()))));
    }

    private Map<String, Object> eventMap(Event e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("eventName", e.getEventName());
        m.put("category", e.getCategory());
        m.put("eventType", e.getEventType());
        m.put("eventDate", e.getEventDate());
        m.put("eventTime", e.getEventTime());
        m.put("venueName", e.getVenueName());
        m.put("location", e.getLocation());
        m.put("collegeName", e.getCollegeName());
        m.put("departmentName", e.getDepartmentName());
        m.put("ticketPrice", e.getTicketPrice());
        m.put("availableSeats", e.getAvailableSeats());
        m.put("totalSeats", e.getTotalSeats());
        m.put("eventBanner", e.getEventBanner());
        m.put("hasCertificate", e.isHasCertificate());
        m.put("organizerName", e.getOrganizer() != null ? e.getOrganizer().getOrganizerName() : null);
        m.put("matchScore", 0);
        m.put("reason", "");
        return m;
    }

    /**
     * Returns or creates the user interest profile.
     */
    @Transactional
    public UserInterest getOrCreateInterest(Long userId, User user) {
        return interestRepository.findByUserId(userId).orElseGet(() -> {
            UserInterest ui = UserInterest.builder().user(user).build();
            return interestRepository.save(ui);
        });
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    public UserInterest updateInterest(Long userId, UserInterest update) {
        UserInterest existing = interestRepository.findByUserId(userId)
                .orElse(UserInterest.builder().user(update.getUser()).build());
        existing.setSkills(update.getSkills());
        existing.setInterests(update.getInterests());
        existing.setDepartment(update.getDepartment());
        existing.setCollege(update.getCollege());
        existing.setYearOfStudy(update.getYearOfStudy());
        existing.setFavoriteCategories(update.getFavoriteCategories());
        existing.setPreferredEventType(update.getPreferredEventType());
        existing.setCareerGoal(update.getCareerGoal());
        return interestRepository.save(existing);
    }
}
