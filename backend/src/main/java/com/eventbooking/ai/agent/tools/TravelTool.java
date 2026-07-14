package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.function.Function;

@Component("travelTool")
@org.springframework.context.annotation.Description("Query travel directions, transport options, hotels, and restaurants near the event venue")
@RequiredArgsConstructor
public class TravelTool implements AgentTool, Function<Map<String, Object>, Map<String, Object>> {

    private final AIService aiService;

    @Override
    public String name() { return "travelTool"; }

    @Override
    public String description() {
        return "Generates travel guide for reaching an event venue: transport options, cab estimate, parking, nearby hotels, restaurants, and weather note.";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long eventId = longVal(input, "eventId");
        if (eventId == null) {
            result.put("error", "eventId is required to generate a travel guide.");
            result.put("prompt", "Which event are you asking about travel directions for?");
            return result;
        }
        try {
            Map<String, Object> travelInfo = aiService.generateTravelInfo(eventId);
            result.putAll(travelInfo);
        } catch (Exception ex) {
            result.put("error", "Could not generate travel info: " + ex.getMessage());
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

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {
        return execute(input, null);
    }
}
