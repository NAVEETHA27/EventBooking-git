package com.eventbooking.service;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.EventPrediction;
import com.eventbooking.entity.EventRating;
import com.eventbooking.entity.SentimentAnalysis;
import com.eventbooking.repository.EventPredictionRepository;
import com.eventbooking.repository.EventRatingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.SentimentAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventGptPhaseService {
    private final EventRepository eventRepository;
    private final EventRatingRepository ratingRepository;
    private final SentimentAnalysisRepository sentimentRepository;
    private final EventPredictionRepository predictionRepository;
    private final OrganizerAnalyticsService organizerAnalyticsService;
    private final AIService aiService;

    @Transactional(readOnly = true)
    public Map<String, Object> travelPlan(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId",    eventId);
        result.put("eventName",  event.getEventName());
        result.put("eventDate",  event.getEventDate());
        result.put("eventTime",  event.getEventTime());
        result.put("venueName",  valueOr(event.getVenueName(), null));
        result.put("venueAddress", valueOr(event.getLocation(), null));
        result.put("googleMapsUrl", valueOr(event.getGoogleMapsUrl(), null));
        result.put("whatsappGroupLink", valueOr(event.getWhatsappGroupLink(), null));
        result.put("whatsappContactNumber", valueOr(event.getWhatsappContactNumber(), null));
        // Transportation
        result.put("nearestBusStop",          valueOr(event.getNearestBusStop(), null));
        result.put("distanceFromBusStop",     valueOr(event.getDistanceFromBusStop(), null));
        result.put("busNumbers",              valueOr(event.getBusNumbers(), null));
        result.put("nearestRailwayStation",   valueOr(event.getNearestRailwayStation(), null));
        result.put("distanceFromRailwayStation", valueOr(event.getDistanceFromRailwayStation(), null));
        result.put("nearestAirport",          valueOr(event.getNearestAirport(), null));
        result.put("metroInformation",        valueOr(event.getMetroInformation(), null));
        result.put("landmarks",               valueOr(event.getLandmarks(), null));
        result.put("parkingAvailable",        valueOr(event.getParkingAvailable(), null));
        result.put("travelGuide",             valueOr(event.getTravelGuide(), null));
        result.put("estimatedTravelTime",     valueOr(event.getEstimatedTravelTime(), null));
        result.put("cabEstimate",             valueOr(event.getCabEstimate(), null));
        // Food
        result.put("foodProvided",            Boolean.TRUE.equals(event.getFoodProvided()));
        result.put("foodMeals",               valueOr(event.getFoodMeals(), null));
        result.put("foodType",                valueOr(event.getFoodType(), null));
        result.put("teaCoffeeProvided",       Boolean.TRUE.equals(event.getTeaCoffeeProvided()));
        result.put("specialDiet",             valueOr(event.getSpecialDiet(), null));
        // Accommodation
        result.put("accommodationProvided",   Boolean.TRUE.equals(event.getAccommodationProvided()));
        result.put("accommodationType",       valueOr(event.getAccommodationType(), null));
        result.put("accommodationCharges",    event.getAccommodationCharges());
        result.put("accommodationBedsAvailable", event.getAccommodationBedsAvailable());
        result.put("accommodationCheckIn",    valueOr(event.getAccommodationCheckIn(), null));
        result.put("accommodationCheckOut",   valueOr(event.getAccommodationCheckOut(), null));
        result.put("accommodationContactPerson", valueOr(event.getAccommodationContactPerson(), null));
        result.put("boysHostelAvailable",     Boolean.TRUE.equals(event.getBoysHostelAvailable()));
        result.put("girlsHostelAvailable",    Boolean.TRUE.equals(event.getGirlsHostelAvailable()));
        result.put("hotelTieupAvailable",     Boolean.TRUE.equals(event.getHotelTieupAvailable()));
        // Nearby
        result.put("nearbyHotels",       valueOr(event.getNearbyHotels(), null));
        result.put("nearbyRestaurants",  valueOr(event.getNearbyRestaurants(), null));
        result.put("emergencyContacts",  valueOr(event.getEmergencyContacts(), null));
        // Reporting
        result.put("reportingTime", event.getReportingTime());
        result.put("dressCode",     valueOr(event.getDressCode(), null));
        result.put("guardrail",     "This guidance uses only organizer-provided event travel information. No data is invented.");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> eventQuestionAnswer(Long eventId, String question) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String answer;
        if (q.contains("food") || q.contains("lunch") || q.contains("dinner") || q.contains("breakfast")) {
            answer = Boolean.TRUE.equals(event.getFoodProvided())
                    ? "Yes. Food is available. Meals: " + valueOr(event.getFoodMeals(), "details will be shared by the organizer")
                    : "Food is not marked as included for this event.";
        } else if (q.contains("accommodation") || q.contains("hostel") || q.contains("stay")) {
            answer = Boolean.TRUE.equals(event.getAccommodationProvided())
                    ? "Accommodation is available. " + valueOr(event.getAccommodationDetails(), valueOr(event.getAccommodationType(), "Contact the organizer for details."))
                    : "Accommodation is not marked as available for this event.";
        } else if (q.contains("bus")) {
            answer = "Nearest bus stop: " + valueOr(event.getNearestBusStop(), "not provided by the organizer.")
                    + ". Bus numbers: " + valueOr(event.getBusNumbers(), "not provided by the organizer.")
                    + ". Distance: " + valueOr(event.getDistanceFromBusStop(), "not provided by the organizer.");
        } else if (q.contains("rail") || q.contains("train")) {
            answer = "Nearest railway station: " + valueOr(event.getNearestRailwayStation(), "not provided by the organizer.")
                    + ". Distance: " + valueOr(event.getDistanceFromRailwayStation(), "not provided by the organizer.");
        } else if (q.contains("metro")) {
            answer = "Metro information: " + valueOr(event.getMetroInformation(), "not provided by the organizer.");
        } else if (q.contains("parking")) {
            answer = "Parking: " + valueOr(event.getParkingAvailable(), "not provided by the organizer.");
        } else if (q.contains("venue") || q.contains("where") || q.contains("reach") || q.contains("landmark") || q.contains("route")) {
            answer = "Venue: " + valueOr(event.getVenueName(), valueOr(event.getLocation(), "not provided by the organizer."))
                    + ". Address: " + valueOr(event.getLocation(), "not provided by the organizer.")
                    + ". Travel guide: " + valueOr(event.getTravelGuide(), "not provided by the organizer.")
                    + ". Landmarks: " + valueOr(event.getLandmarks(), "not provided by the organizer.");
        } else if (q.contains("beginner")) {
            answer = "Beginner friendliness depends on the agenda. Category: " + event.getCategory() + ". Check the description and organizer notes.";
        } else if (q.contains("bring") || q.contains("pack")) {
            answer = "Suggested items: " + String.join(", ", packingItems(event.getCategory()));
        } else {
            answer = "I can answer only questions about this event using organizer-provided details. Ask about food, travel, accommodation, parking, schedule, certificates, rules, or what to bring.";
        }
        return Map.of("eventId", eventId, "question", question, "answer", answer, "ragReady", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> packingChecklist(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        return Map.of("eventId", eventId, "category", event.getCategory(), "items", packingItems(event.getCategory()), "ragReady", true);
    }

    @Transactional
    public Map<String, Object> analyzeFeedback(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        List<EventRating> reviews = ratingRepository.findByEventIdAndModerationStatus(eventId, EventRating.ModerationStatus.APPROVED);

        int positive = 0;
        int neutral = 0;
        int negative = 0;
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<Long> spamReviewIds = new ArrayList<>();
        Map<String, Long> duplicateText = reviews.stream()
                .filter(review -> review.getReviewText() != null)
                .collect(Collectors.groupingBy(review -> normalize(review.getReviewText()), Collectors.counting()));

        for (EventRating review : reviews) {
            if (review.getOverallRating() >= 4) {
                positive++;
                strengths.add(firstSentence(review.getReviewText(), "Strong attendee satisfaction"));
            } else if (review.getOverallRating() <= 2) {
                negative++;
                weaknesses.add(firstSentence(review.getReviewText(), "Low attendee satisfaction"));
            } else {
                neutral++;
            }
            if (isSpam(review, duplicateText)) {
                spamReviewIds.add(review.getId());
            }
        }

        if (strengths.isEmpty()) strengths.add("Audience engagement needs more data.");
        if (weaknesses.isEmpty()) weaknesses.add("No major negative pattern detected.");
        suggestions.add("Improve pre-event communication and reminders.");
        suggestions.add("Collect more detailed post-event feedback.");
        suggestions.add("Track attendance against registrations for better planning.");

        double average = reviews.stream().mapToInt(EventRating::getOverallRating).average().orElse(0.0);
        double satisfaction = Math.round(average * 20.0) / 10.0;

        SentimentAnalysis analysis = sentimentRepository.findByEventId(eventId).orElseGet(SentimentAnalysis::new);
        analysis.setEvent(event);
        analysis.setPositiveCount(positive);
        analysis.setNeutralCount(neutral);
        analysis.setNegativeCount(negative);
        analysis.setSatisfactionScore(satisfaction);
        analysis.setStrengths(toJsonLike(strengths));
        analysis.setWeaknesses(toJsonLike(weaknesses));
        analysis.setImprovementSuggestions(toJsonLike(suggestions));
        analysis.setImprovementReport("Positive: " + positive + ", neutral: " + neutral + ", negative: " + negative + ". " +
                "Organizer should focus on communication, schedule clarity, and feedback collection.");
        analysis.setAiTestimonial(strengths.get(0));
        analysis.setTotalReviewsAnalyzed(reviews.size());
        analysis.setUpdatedAt(LocalDateTime.now());
        sentimentRepository.save(analysis);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", eventId);
        result.put("eventName", event.getEventName());
        result.put("sentiment", Map.of("positive", positive, "neutral", neutral, "negative", negative));
        result.put("positiveAspects", strengths.stream().distinct().limit(5).toList());
        result.put("negativeAspects", weaknesses.stream().distinct().limit(5).toList());
        result.put("improvementSuggestions", suggestions);
        result.put("organizerScore", satisfaction);
        result.put("eventScore", satisfaction);
        result.put("reviewSummary", analysis.getImprovementReport());
        result.put("spamReviewIds", spamReviewIds);
        result.put("duplicateReviewCount", duplicateText.values().stream().filter(count -> count > 1).mapToLong(Long::longValue).sum());
        return result;
    }

    @Transactional
    public Map<String, Object> predict(Long eventId, Long organizerId) {
        Map<String, Object> raw = organizerAnalyticsService.predictEventOutcomes(eventId, organizerId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        int registrations = number(raw.get("currentRegistrations"));
        int predictedAttendance = number(raw.get("predictedAttendance"));
        BigDecimal revenue = raw.get("predictedRevenue") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
        int certificateCount = event.isHasCertificate() ? predictedAttendance : 0;

        EventPrediction prediction = predictionRepository.findByEventId(eventId).orElseGet(EventPrediction::new);
        prediction.setEvent(event);
        prediction.setPredictedRegistrations(Math.max(registrations, predictedAttendance));
        prediction.setPredictedAttendance(predictedAttendance);
        prediction.setPredictedNoShowRate(15.0);
        prediction.setPredictedRevenue(revenue);
        prediction.setPredictedFoodCount(Boolean.TRUE.equals(event.getFoodProvided()) ? predictedAttendance : 0);
        prediction.setPredictedCertificateCount(certificateCount);
        prediction.setSuccessScore(((Number) raw.getOrDefault("successScore", 0)).doubleValue());
        prediction.setAiSuggestions(String.valueOf(raw.getOrDefault("aiPrediction", "Promote early and send reminders to reduce no-shows.")));
        prediction.setExpectedRevenue(revenue);
        prediction.setBudgetEstimate(revenue.multiply(BigDecimal.valueOf(0.55)));
        prediction.setBreakEvenPoint(revenue.multiply(BigDecimal.valueOf(0.55)));
        prediction.setEstimatedProfit(revenue.multiply(BigDecimal.valueOf(0.45)));
        prediction.setUpdatedAt(LocalDateTime.now());
        predictionRepository.save(prediction);

        Map<String, Object> result = new LinkedHashMap<>(raw);
        result.put("foodRequirement", prediction.getPredictedFoodCount());
        result.put("certificateCount", certificateCount);
        result.put("capacity", event.getTotalSeats());
        result.put("engagementScore", Math.min(100, Math.round((registrations / Math.max(1.0, event.getTotalSeats())) * 100)));
        result.put("storedPredictionId", prediction.getId());
        return result;
    }

    public Map<String, Object> phaseStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("phase5RecommendationAI", "Implemented through RecommendationService and /recommendations/discover.");
        status.put("phase6TravelAI", "Implemented through /ai/eventgpt/events/{id}/travel-plan.");
        status.put("phase7NlpSearch", "Implemented through /ai/search with fallback event ranking.");
        status.put("phase8FeedbackAI", "Implemented through /ai/eventgpt/events/{id}/feedback-analysis.");
        status.put("phase9CertificateAI", "Implemented through certificate generation endpoints and agent tool queue.");
        status.put("phase10PredictionAI", "Implemented through /ai/eventgpt/events/{id}/prediction.");
        status.put("phase11PortfolioAI", "Implemented through PortfolioService and /portfolio/my.");
        status.put("phase12NetworkingAI", "Implemented through NetworkingService and /networking/suggestions.");
        return status;
    }

    private boolean isSpam(EventRating review, Map<String, Long> duplicateText) {
        String text = review.getReviewText();
        if (text == null || text.isBlank()) return true;
        String normalized = normalize(text);
        return text.length() < 12 || duplicateText.getOrDefault(normalized, 0L) > 1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String firstSentence(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String trimmed = value.trim();
        int end = trimmed.indexOf('.');
        String sentence = end > 0 ? trimmed.substring(0, end + 1) : trimmed;
        return sentence.length() > 160 ? sentence.substring(0, 160) : sentence;
    }

    private String toJsonLike(List<String> values) {
        return values.stream()
                .distinct()
                .limit(8)
                .map(value -> "\"" + value.replace("\"", "'") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<String> packingItems(String category) {
        String cat = category == null ? "" : category.toUpperCase(Locale.ROOT);
        List<String> items = new ArrayList<>(List.of("College ID", "Notebook", "Water Bottle", "Phone Charger"));
        if (cat.contains("HACKATHON") || cat.contains("CODING") || cat.contains("WORKSHOP") || cat.contains("TECH")) {
            items.addAll(List.of("Laptop", "Laptop Charger", "Extension Cord", "Project Files"));
        }
        if (cat.contains("SPORTS")) items.addAll(List.of("Sports Shoes", "Towel", "Change of Clothes"));
        if (cat.contains("CULTURAL")) items.addAll(List.of("Costume or Props", "Practice Track", "Safety Pins"));
        return items.stream().distinct().toList();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int number(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
