package com.eventbooking.service;

import com.eventbooking.entity.*;
import com.eventbooking.exception.BookingException;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service handling ratings, sentiment analysis, fake review detection, and testimonials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final EventRatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final SentimentAnalysisRepository sentimentRepository;
    private final com.eventbooking.ai.AIEngine aiEngine;
    private final GamificationService gamificationService;

    /**
     * Submit a rating. Only verified attendees can submit. One review per user per event.
     */
    @Transactional
    public EventRating submitRating(Long userId, Long eventId, EventRating rating) {
        validateFeedback(rating);
        // Verify event exists and is completed
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BookingException("Event not found"));
        if (!isEventEnded(event)) {
            throw new BookingException("Feedback opens only after the event has completed");
        }

        Booking booking = bookingRepository.findConfirmedByUserAndEvent(userId, eventId)
                .orElseThrow(() -> new BookingException("Only registered participants can review this event"));
        // Check attendance (QR check-in required)
        boolean attended = attendanceRepository.existsByBookingUserIdAndBookingEventId(userId, eventId);
        if (!attended) {
            throw new BookingException("Only QR verified attendees can review this event");
        }

        if (ratingRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BookingException("You have already submitted a review for this event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BookingException("User not found"));

        rating.setEvent(event);
        rating.setUser(user);
        rating.setBooking(booking);
        rating.setVerifiedAttendance(attended);
        rating.setModerationStatus(EventRating.ModerationStatus.APPROVED);
        sanitizeFeedbackText(rating);

        // Basic fake detection
        detectFakeReview(rating);

        EventRating saved = ratingRepository.save(rating);

        // Async: analyze sentiment and update aggregates
        analyzeSentimentAsync(eventId);
        gamificationService.awardReviewXp(userId);

        return saved;
    }

    @Transactional
    public EventRating updateRating(Long userId, Long eventId, EventRating incoming) {
        validateFeedback(incoming);
        EventRating rating = ratingRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BookingException("Review not found"));
        if (rating.getCreatedAt() == null || rating.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now())) {
            throw new BookingException("Reviews can be edited for 7 days only");
        }
        rating.setOverallRating(incoming.getOverallRating());
        rating.setSpeakerRating(incoming.getSpeakerRating());
        rating.setVenueRating(incoming.getVenueRating());
        rating.setFoodRating(incoming.getFoodRating());
        rating.setOrganizationRating(incoming.getOrganizationRating());
        rating.setEventQualityRating(incoming.getEventQualityRating());
        rating.setAccommodationRating(incoming.getAccommodationRating());
        rating.setContentQualityRating(incoming.getContentQualityRating());
        rating.setValueForMoneyRating(incoming.getValueForMoneyRating());
        rating.setReviewText(incoming.getReviewText());
        rating.setSuggestions(incoming.getSuggestions());
        rating.setAnonymous(incoming.isAnonymous());
        sanitizeFeedbackText(rating);
        detectFakeReview(rating);
        EventRating saved = ratingRepository.save(rating);
        analyzeSentimentAsync(eventId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFeedbackEligibility(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BookingException("Event not found"));
        boolean registered = bookingRepository.findConfirmedByUserAndEvent(userId, eventId).isPresent();
        boolean attended = attendanceRepository.existsByBookingUserIdAndBookingEventId(userId, eventId);
        boolean eventEnded = isEventEnded(event);
        Optional<EventRating> review = ratingRepository.findByEventIdAndUserId(eventId, userId);
        boolean canEdit = review.map(r -> r.getCreatedAt() != null
                && !r.getCreatedAt().plusDays(7).isBefore(LocalDateTime.now())).orElse(false);
        boolean canSubmit = registered && attended && eventEnded && review.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registered", registered);
        result.put("attended", attended);
        result.put("eventEnded", eventEnded);
        result.put("hasReview", review.isPresent());
        result.put("canSubmit", canSubmit);
        result.put("canEdit", canEdit);
        result.put("editDeadline", review.map(r -> r.getCreatedAt() == null ? null : r.getCreatedAt().plusDays(7)).orElse(null));
        return result;
    }

    @Transactional(readOnly = true)
    public Page<EventRating> getEventRatings(Long eventId, int page, int size) {
        return ratingRepository.findByEventIdAndModerationStatusOrderByCreatedAtDesc(
                eventId, EventRating.ModerationStatus.APPROVED, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventRatingSummary(Long eventId) {
        List<EventRating> ratings = ratingRepository.findByEventIdAndModerationStatus(
                eventId, EventRating.ModerationStatus.APPROVED);
        if (ratings.isEmpty()) return Map.of(
                "averageRating", 0.0,
                "totalReviews", 0,
                "ratingDistribution", Map.of(),
                "categoryAverages", Map.of(),
                "positiveComments", List.of(),
                "improvementSuggestionsList", List.of());

        double avg = ratings.stream().mapToInt(EventRating::getOverallRating).average().orElse(0.0);
        Map<Integer, Long> dist = ratings.stream()
                .collect(Collectors.groupingBy(EventRating::getOverallRating, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageRating", Math.round(avg * 10.0) / 10.0);
        result.put("totalReviews", ratings.size());
        result.put("ratingDistribution", dist);

        Map<String, Double> categoryAverages = new LinkedHashMap<>();
        putAverage(categoryAverages, "eventQuality", ratings.stream().map(EventRating::getEventQualityRating).filter(Objects::nonNull).toList());
        putAverage(categoryAverages, "speaker", ratings.stream().map(EventRating::getSpeakerRating).filter(Objects::nonNull).toList());
        putAverage(categoryAverages, "venue", ratings.stream().map(EventRating::getVenueRating).filter(Objects::nonNull).toList());
        putAverage(categoryAverages, "organization", ratings.stream().map(EventRating::getOrganizationRating).filter(Objects::nonNull).toList());
        putAverage(categoryAverages, "food", ratings.stream().map(EventRating::getFoodRating).filter(Objects::nonNull).toList());
        putAverage(categoryAverages, "accommodation", ratings.stream().map(EventRating::getAccommodationRating).filter(Objects::nonNull).toList());
        result.put("categoryAverages", categoryAverages);
        result.put("positiveComments", ratings.stream()
                .filter(r -> r.getOverallRating() >= 4 && StringUtils.hasText(r.getReviewText()))
                .limit(5)
                .map(r -> Map.of("comment", r.getReviewText(), "author", displayName(r)))
                .toList());
        result.put("improvementSuggestionsList", ratings.stream()
                .filter(r -> StringUtils.hasText(r.getSuggestions()))
                .limit(5)
                .map(r -> Map.of("suggestion", r.getSuggestions(), "author", displayName(r)))
                .toList());

        // Sentiment analysis
        sentimentRepository.findByEventId(eventId).ifPresent(s -> {
            result.put("satisfactionScore", s.getSatisfactionScore());
            result.put("strengths", s.getStrengths());
            result.put("weaknesses", s.getWeaknesses());
            result.put("improvementSuggestions", s.getImprovementSuggestions());
            result.put("aiTestimonial", s.getAiTestimonial());
        });

        return result;
    }

    /**
     * Async: run AI sentiment analysis on all reviews for an event.
     * NOTE: All lazy association access is resolved within the transaction boundary
     * before being passed to helper methods that run after the entity is detached.
     */
    @Async
    @Transactional
    public void analyzeSentimentAsync(Long eventId) {
        try {
            List<EventRating> ratings = ratingRepository.findByEventIdAndModerationStatus(
                    eventId, EventRating.ModerationStatus.APPROVED);
            if (ratings.isEmpty()) return;

            // Simple rule-based sentiment classification
            for (EventRating r : ratings) {
                String sentiment = classifySentiment(r.getOverallRating(), r.getReviewText());
                r.setSentiment(sentiment);
                r.setSentimentScore(r.getOverallRating() / 5.0);
                ratingRepository.save(r);
            }

            long positive = ratings.stream().filter(r -> "POSITIVE".equals(r.getSentiment())).count();
            long neutral  = ratings.stream().filter(r -> "NEUTRAL".equals(r.getSentiment())).count();
            long negative = ratings.stream().filter(r -> "NEGATIVE".equals(r.getSentiment())).count();

            double satisfactionScore = ratings.stream()
                    .mapToInt(EventRating::getOverallRating).average().orElse(0.0) * 2; // /5 * 10

            // Generate AI insights
            String aiInsights = generateSentimentInsights(eventId, ratings);

            SentimentAnalysis sa = sentimentRepository.findByEventId(eventId)
                    .orElseGet(() -> {
                        Event ev = new Event();
                        ev.setId(eventId);
                        return SentimentAnalysis.builder().event(ev).build();
                    });

            sa.setPositiveCount((int) positive);
            sa.setNeutralCount((int) neutral);
            sa.setNegativeCount((int) negative);
            sa.setSatisfactionScore(Math.round(satisfactionScore * 100.0) / 100.0);
            sa.setTotalReviewsAnalyzed(ratings.size());
            sa.setUpdatedAt(LocalDateTime.now());

            if (aiInsights != null) {
                // Parse AI response into sections (simplified)
                sa.setImprovementReport(aiInsights);
                sa.setStrengths(extractSection(aiInsights, "Strengths", "Weaknesses"));
                sa.setWeaknesses(extractSection(aiInsights, "Weaknesses", "Improvements"));
                sa.setImprovementSuggestions(extractSection(aiInsights, "Improvements", null));
            }

            // Generate testimonial from top-rated reviews.
            // Eagerly resolve lazy user.name INSIDE this @Transactional method to avoid
            // LazyInitializationException after the session closes.
            List<EventRating> top = ratingRepository.findTopRatingsByEventId(
                    eventId, PageRequest.of(0, 3));
            if (!top.isEmpty()) {
                // Build a plain-string version of reviews so we never access lazy proxies outside Tx
                String testimonialInput = top.stream()
                        .filter(r -> StringUtils.hasText(r.getReviewText()))
                        .map(r -> {
                            // Access user.name while session is open
                            String userName = (r.getUser() != null) ? r.getUser().getName() : "Attendee";
                            return userName + " (" + r.getOverallRating() + "/5): " + r.getReviewText();
                        })
                        .collect(Collectors.joining("\n"));
                sa.setAiTestimonial(generateTestimonialFromText(testimonialInput));
            }

            sentimentRepository.save(sa);

        } catch (Exception ex) {
            log.warn("Sentiment analysis failed for event {}: {}", eventId, ex.getMessage());
        }
    }

    private String classifySentiment(int rating, String text) {
        if (rating >= 4) return "POSITIVE";
        if (rating == 3) return "NEUTRAL";
        return "NEGATIVE";
    }

    private boolean isEventEnded(Event event) {
        if (event.getStatus() == Event.EventStatus.COMPLETED || event.getStatus() == Event.EventStatus.EXPIRED) {
            return true;
        }
        LocalDate endDate = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();
        return endDate != null && endDate.isBefore(LocalDate.now());
    }

    private String generateSentimentInsights(Long eventId, List<EventRating> ratings) {
        if (aiEngine == null || !aiEngine.isAvailable()) return null;
        try {
            String reviewText = ratings.stream()
                    .filter(r -> StringUtils.hasText(r.getReviewText()))
                    .limit(10)
                    .map(r -> "Rating " + r.getOverallRating() + "/5: " + r.getReviewText()
                            + (StringUtils.hasText(r.getSuggestions()) ? "\nSuggestion: " + r.getSuggestions() : ""))
                    .collect(Collectors.joining("\n"));
            if (!StringUtils.hasText(reviewText)) return null;
            String prompt = """
                    Analyze these event reviews and provide:
                    ## Strengths
                    (bullet list of what worked well)
                    ## Weaknesses
                    (bullet list of issues)
                    ## Improvements
                    (bullet list of specific suggestions for next time)
                    
                    Reviews:
                    """ + reviewText;
            return aiEngine.complete(
                    "SENTIMENT",
                    "You are an event quality analyst. Be concise and constructive.",
                    prompt);
        } catch (Exception ex) {
            log.warn("AI sentiment generation failed: {}", ex.getMessage());
            return null;
        }
    }

    private String generateTestimonialFromText(String reviews) {
        if (aiEngine == null || !aiEngine.isAvailable() || !StringUtils.hasText(reviews)) return null;
        try {
            return aiEngine.complete(
                    "SENTIMENT_TESTIMONIAL",
                    "You are a professional marketing writer. Create a single compelling 2-sentence testimonial from these reviews for the event page. Be genuine and specific.",
                    reviews);
        } catch (Exception ex) {
            return null;
        }
    }

    private String generateTestimonial(List<EventRating> topRatings) {
        if (aiEngine == null || !aiEngine.isAvailable() || topRatings.isEmpty()) return null;
        String reviews = topRatings.stream()
                .filter(r -> StringUtils.hasText(r.getReviewText()))
                .map(r -> {
                    String name = (r.getUser() != null) ? r.getUser().getName() : "Attendee";
                    return name + " (" + r.getOverallRating() + "/5): " + r.getReviewText();
                })
                .collect(Collectors.joining("\n"));
        return generateTestimonialFromText(reviews);
    }

    private void detectFakeReview(EventRating rating) {
        // Simple heuristics: very short text on 5-star or 1-star from unverified user
        if (!rating.isVerifiedAttendance() && rating.getOverallRating() == 5) {
            if (rating.getReviewText() == null || rating.getReviewText().length() < 10) {
                rating.setFakeFlagged(true);
                rating.setModerationStatus(EventRating.ModerationStatus.FLAGGED);
            }
        }
    }

    private void validateFeedback(EventRating rating) {
        if (rating == null) throw new BookingException("Feedback is required");
        if (rating.getOverallRating() < 1 || rating.getOverallRating() > 5) {
            throw new BookingException("Overall rating must be between 1 and 5");
        }
        validateOptionalRating("Event quality", rating.getEventQualityRating());
        validateOptionalRating("Speaker quality", rating.getSpeakerRating());
        validateOptionalRating("Venue", rating.getVenueRating());
        validateOptionalRating("Organization", rating.getOrganizationRating());
        validateOptionalRating("Food", rating.getFoodRating());
        validateOptionalRating("Accommodation", rating.getAccommodationRating());
        validateLength("Comments", rating.getReviewText(), 2000);
        validateLength("Suggestions", rating.getSuggestions(), 2000);
    }

    private void validateOptionalRating(String label, Integer value) {
        if (value != null && (value < 1 || value > 5)) {
            throw new BookingException(label + " rating must be between 1 and 5");
        }
    }

    private void validateLength(String label, String value, int max) {
        if (value != null && value.length() > max) {
            throw new BookingException(label + " must be " + max + " characters or less");
        }
    }

    private void sanitizeFeedbackText(EventRating rating) {
        if (rating.getReviewText() != null) rating.setReviewText(rating.getReviewText().trim());
        if (rating.getSuggestions() != null) rating.setSuggestions(rating.getSuggestions().trim());
    }

    private void putAverage(Map<String, Double> map, String key, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        map.put(key, Math.round(avg * 10.0) / 10.0);
    }

    public Map<String, Object> toPublicReview(EventRating rating) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rating.getId());
        map.put("overallRating", rating.getOverallRating());
        map.put("eventQualityRating", rating.getEventQualityRating());
        map.put("speakerRating", rating.getSpeakerRating());
        map.put("venueRating", rating.getVenueRating());
        map.put("organizationRating", rating.getOrganizationRating());
        map.put("foodRating", rating.getFoodRating());
        map.put("accommodationRating", rating.getAccommodationRating());
        map.put("reviewText", rating.getReviewText());
        map.put("suggestions", rating.getSuggestions());
        map.put("anonymous", rating.isAnonymous());
        map.put("authorName", displayName(rating));
        map.put("verifiedAttendance", rating.isVerifiedAttendance());
        map.put("createdAt", rating.getCreatedAt());
        return map;
    }

    private String displayName(EventRating rating) {
        if (rating == null || rating.isAnonymous() || rating.getUser() == null) return "Anonymous attendee";
        return StringUtils.hasText(rating.getUser().getName()) ? rating.getUser().getName() : "Verified attendee";
    }

    private String extractSection(String text, String from, String to) {
        if (text == null) return null;
        int start = text.indexOf("## " + from);
        if (start == -1) return null;
        start = text.indexOf("\n", start) + 1;
        int end = to != null ? text.indexOf("## " + to, start) : text.length();
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    @Transactional
    public EventRating moderateReview(Long ratingId, EventRating.ModerationStatus status, String note) {
        EventRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new BookingException("Review not found"));
        rating.setModerationStatus(status);
        rating.setModerationNote(note);
        return ratingRepository.save(rating);
    }

    @Transactional(readOnly = true)
    public Optional<EventRating> getUserReview(Long userId, Long eventId) {
        return ratingRepository.findByEventIdAndUserId(eventId, userId);
    }
}
