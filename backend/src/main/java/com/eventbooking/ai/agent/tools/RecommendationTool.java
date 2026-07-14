package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecommendationTool implements AgentTool {

    private final RecommendationService recommendationService;

    @Override
    public String name() { return "recommendationTool"; }

    @Override
    public String description() {
        return "Generates personalized event recommendations: trending, AI picks, recommended for you, free events, upcoming this week.";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Long userId = principal != null && "USER".equalsIgnoreCase(principal.getRole())
                    ? principal.getId() : null;
            Map<String, java.util.List<Map<String, Object>>> recommendations =
                    recommendationService.getDiscoverRecommendations(userId, null, null);

            // Surface top 5 from each relevant category
            if (recommendations.containsKey(RecommendationService.RECOMMENDED_FOR_YOU)) {
                result.put("recommendedForYou", recommendations.get(RecommendationService.RECOMMENDED_FOR_YOU)
                        .stream().limit(5).toList());
            }
            if (recommendations.containsKey(RecommendationService.TRENDING)) {
                result.put("trending", recommendations.get(RecommendationService.TRENDING)
                        .stream().limit(5).toList());
            }
            if (recommendations.containsKey(RecommendationService.UPCOMING_THIS_WEEK)) {
                result.put("upcomingThisWeek", recommendations.get(RecommendationService.UPCOMING_THIS_WEEK)
                        .stream().limit(5).toList());
            }
            if (recommendations.containsKey(RecommendationService.FREE_EVENTS)) {
                result.put("freeEvents", recommendations.get(RecommendationService.FREE_EVENTS)
                        .stream().limit(4).toList());
            }
            if (recommendations.containsKey(RecommendationService.HIGHEST_RATED)) {
                result.put("highestRated", recommendations.get(RecommendationService.HIGHEST_RATED)
                        .stream().limit(4).toList());
            }
            result.put("personalised", userId != null);
        } catch (Exception ex) {
            result.put("error", "Could not load recommendations: " + ex.getMessage());
        }
        return result;
    }
}
