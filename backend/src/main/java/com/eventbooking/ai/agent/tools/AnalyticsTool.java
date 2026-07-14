package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.OrganizerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.function.Function;

@Component("analyticsTool")
@org.springframework.context.annotation.Description("Expose system analytics dashboard: events, bookings, and revenue metrics")
@RequiredArgsConstructor
public class AnalyticsTool implements AgentTool, Function<Map<String, Object>, Map<String, Object>> {

    private final OrganizerAnalyticsService analyticsService;

    @Override
    public String name() { return "analyticsTool"; }

    @Override
    public String description() {
        return "Fetches organizer analytics: total events, registrations, attendance, revenue, performance score, and badge.";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (principal == null || !"ORGANIZER".equalsIgnoreCase(principal.getRole())) {
            result.put("error", "Organizer login required to access analytics.");
            return result;
        }
        try {
            Map<String, Object> analytics = analyticsService.getOrganizerAnalytics(principal.getId());
            // Surface key metrics concisely
            result.put("totalEvents", analytics.get("totalEvents"));
            result.put("completedEvents", analytics.get("completedEvents"));
            result.put("publishedEvents", analytics.get("publishedEvents"));
            result.put("totalRegistrations", analytics.get("totalRegistrations"));
            result.put("totalAttendance", analytics.get("totalAttendance"));
            result.put("categoryDistribution", analytics.get("categoryDistribution"));
            result.put("performanceScore", analytics.get("performanceScore"));
            result.put("aiInsights", analytics.get("aiInsights"));
            // Top 3 events by registrations
            if (analytics.get("eventStats") instanceof java.util.List<?> stats) {
                result.put("topEvents", stats.stream()
                        .sorted((a, b) -> {
                            if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
                                long ra = ma.get("registrations") instanceof Number n ? n.longValue() : 0;
                                long rb = mb.get("registrations") instanceof Number n ? n.longValue() : 0;
                                return Long.compare(rb, ra);
                            }
                            return 0;
                        })
                        .limit(3)
                        .toList());
            }
        } catch (Exception ex) {
            result.put("error", "Could not fetch analytics: " + ex.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {
        return execute(input, null);
    }
}
