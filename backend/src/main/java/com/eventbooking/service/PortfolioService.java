package com.eventbooking.service;

import com.eventbooking.ai.AIProvider;
import com.eventbooking.entity.*;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Student Portfolio — auto-generated from bookings, attendance, certificates, ratings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final BookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final CertificateRepository certificateRepository;
    private final EventRatingRepository ratingRepository;
    private final UserInterestRepository interestRepository;
    private final GamificationService gamificationService;
    private final List<AIProvider> aiProviders;

    @Value("${ai.provider:gemini}")
    private String providerName;

    @Transactional
    public Map<String, Object> getPortfolio(Long userId, User user) {
        Map<String, Object> portfolio = new LinkedHashMap<>();

        log.debug("[Portfolio] Building portfolio for userId={}", userId);

        // Basic info
        portfolio.put("name", user.getName());
        portfolio.put("email", user.getEmail());

        // Interest profile
        UserInterest interest = interestRepository.findByUserId(userId).orElse(null);
        log.debug("[Portfolio] UserInterest loaded: hasInterest={}", interest != null);
        if (interest != null) {
            portfolio.put("department", interest.getDepartment());
            portfolio.put("college", interest.getCollege());
            portfolio.put("yearOfStudy", interest.getYearOfStudy());
            portfolio.put("skills", interest.getSkills());
            portfolio.put("interests", interest.getInterests());
            portfolio.put("careerGoal", interest.getCareerGoal());
        }

        // Bookings / registrations
        var bookings = bookingRepository.findByUserId(userId,
                PageRequest.of(0, 200, Sort.by("bookedAt").descending())).getContent();
        log.debug("[Portfolio] Bookings loaded: count={}", bookings.size());
        portfolio.put("totalRegistrations", bookings.size());

        // Category distribution
        Map<String, Long> categoryDist = bookings.stream()
                .filter(b -> b.getEvent() != null && b.getEvent().getCategory() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getEvent().getCategory(), Collectors.counting()));
        portfolio.put("categoryDistribution", categoryDist);

        // Confirmed bookings count
        List<Booking> confirmedBookings = bookings.stream()
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
        portfolio.put("confirmedBookings", confirmedBookings.size());

        // Certificates
        var certs = certificateRepository.findByUserId(userId);
        log.debug("[Portfolio] Certificates loaded: count={}", certs.size());
        portfolio.put("certificatesEarned", certs.size());
        portfolio.put("certificates", certs.stream().map(c -> Map.of(
                "certificateId", c.getCertificateId(),
                "eventName", c.getEventName(),
                "issuedAt", c.getIssuedAt()
        )).collect(Collectors.toList()));

        // Reviews given
        var reviews = ratingRepository.findByUserId(userId);
        portfolio.put("reviewsGiven", reviews.size());
        double avgGiven = reviews.stream().mapToInt(EventRating::getOverallRating).average().orElse(0.0);
        portfolio.put("averageRatingGiven", Math.round(avgGiven * 10.0) / 10.0);

        // Gamification — requires a writable transaction (may INSERT user_levels row)
        log.debug("[Portfolio] Loading gamification profile");
        portfolio.put("gamification", gamificationService.getGamificationProfile(userId));

        // AI-generated summary
        portfolio.put("aiSummary", generateSummary(user, interest, certs.size(),
                bookings.size(), categoryDist));

        // Skills chart data
        portfolio.put("skillsData", buildSkillsData(categoryDist, interest));

        log.debug("[Portfolio] Portfolio built successfully for userId={}", userId);
        return portfolio;
    }

    private String generateSummary(User user, UserInterest interest, int certs,
                                    int registrations, Map<String, Long> categories) {
        AIProvider provider = selectProvider();
        if (provider == null) return buildFallbackSummary(user, certs, registrations, categories);
        try {
            String top = categories.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
            String prompt = """
                    Generate a 3-sentence professional student portfolio summary for:
                    Name: %s
                    Department: %s
                    Events Registered: %d
                    Certificates Earned: %d
                    Top Categories: %s
                    Career Goal: %s
                    Make it encouraging, professional, and specific.
                    """.formatted(
                    user.getName(),
                    interest != null ? interest.getDepartment() : "N/A",
                    registrations, certs, top,
                    interest != null ? interest.getCareerGoal() : "N/A");
            return provider.complete(
                    "You are a professional career advisor writing student portfolios.", prompt, List.of());
        } catch (Exception ex) {
            log.warn("AI summary failed: {}", ex.getMessage());
            return buildFallbackSummary(user, certs, registrations, categories);
        }
    }

    private String buildFallbackSummary(User user, int certs, int registrations,
                                         Map<String, Long> categories) {
        String top = categories.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(2).map(Map.Entry::getKey).collect(Collectors.joining(" and "));
        return "%s has actively participated in %d events, focusing on %s. With %d certificate(s) earned, they demonstrate a strong commitment to continuous learning and professional growth."
                .formatted(user.getName(), registrations,
                        top.isEmpty() ? "various categories" : top, certs);
    }

    private List<Map<String, Object>> buildSkillsData(Map<String, Long> categoryDist,
                                                        UserInterest interest) {
        List<Map<String, Object>> skills = new ArrayList<>();
        categoryDist.forEach((cat, count) ->
                skills.add(Map.of("name", cat, "value", count)));
        if (interest != null && interest.getSkills() != null) {
            for (String skill : interest.getSkills().split(",")) {
                skills.add(Map.of("name", skill.trim(), "value", 1));
            }
        }
        return skills;
    }

    private AIProvider selectProvider() {
        return aiProviders.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName) && p.isConfigured())
                .findFirst()
                .orElseGet(() -> aiProviders.stream().filter(AIProvider::isConfigured).findFirst().orElse(null));
    }
}
