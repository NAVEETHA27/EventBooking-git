package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.repository.EventRatingRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventGptPhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeedbackTool implements AgentTool {

    private final EventRatingRepository ratingRepository;
    private final EventGptPhaseService eventGptPhaseService;

    @Override
    public String name() { return "feedbackTool"; }

    @Override
    public String description() {
        return "Fetches event feedback sentiment analysis, satisfaction scores, strengths, weaknesses, and improvement suggestions for organizers.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long eventId = longVal(input, "eventId");

        if (eventId == null) {
            result.put("error", "eventId is required for feedback analysis.");
            result.put("prompt", "Which event's feedback would you like to analyse?");
            return result;
        }

        boolean isOrganizer = principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole());
        boolean isAdmin = principal != null && "ADMIN".equalsIgnoreCase(principal.getRole());

        if (!isOrganizer && !isAdmin) {
            // Students: just show average rating
            try {
                double avg = ratingRepository.findAverageRatingByEventId(eventId).orElse(0.0);
                long count = ratingRepository.countApprovedByEventId(eventId);
                result.put("averageRating", Math.round(avg * 10.0) / 10.0);
                result.put("totalReviews", count);
            } catch (Exception ex) {
                result.put("error", "Could not fetch ratings.");
            }
            return result;
        }

        try {
            Map<String, Object> analysis = eventGptPhaseService.analyzeFeedback(eventId);
            result.putAll(analysis);
        } catch (Exception ex) {
            result.put("error", "Could not perform feedback analysis: " + ex.getMessage());
        }
        return result;
    }

    private Long longVal(Map<String, Object> input, String key) {
        Object v = input.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
