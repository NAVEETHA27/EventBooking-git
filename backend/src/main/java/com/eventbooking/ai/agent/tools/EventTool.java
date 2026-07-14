package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.entity.Event;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.function.Function;

@Component("eventTool")
@org.springframework.context.annotation.Description("Search events on the platform using natural language keywords")
@RequiredArgsConstructor
public class EventTool implements AgentTool, Function<Map<String, Object>, Map<String, Object>> {

    private final EventRepository eventRepository;

    @Override
    public String name() { return "eventTool"; }

    @Override
    public String description() {
        return "Fetches upcoming events, event details by ID, organizer's own events, and event seat/schedule info.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Long eventId = longVal(input, "eventId");
            if (eventId != null) {
                return eventRepository.findById(eventId)
                        .map(e -> {
                            Map<String, Object> m = eventMap(e);
                            result.putAll(m);
                            return result;
                        })
                        .orElseGet(() -> { result.put("error", "Event not found"); return result; });
            }

            // Organizer wants their own events
            if (principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole())) {
                List<Map<String, Object>> events = eventRepository
                        .findByOrganizerId(principal.getId(), PageRequest.of(0, 20, Sort.by("createdAt").descending()))
                        .getContent().stream().map(this::eventMap).collect(Collectors.toList());
                result.put("myEvents", events);
                result.put("count", events.size());
                return result;
            }

            // Public upcoming events
            String keyword = strVal(input, "keyword");
            String category = strVal(input, "category");
            List<Event> upcoming = eventRepository.findUpcomingPublicEvents(LocalDate.now());
            List<Map<String, Object>> filtered = upcoming.stream()
                    .filter(e -> keyword == null || e.getEventName().toLowerCase().contains(keyword.toLowerCase())
                            || (e.getTags() != null && e.getTags().toLowerCase().contains(keyword.toLowerCase()))
                            || (e.getCategory() != null && e.getCategory().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(e -> category == null || category.equalsIgnoreCase(e.getCategory()))
                    .limit(10)
                    .map(this::eventMap)
                    .collect(Collectors.toList());
            result.put("events", filtered);
            result.put("count", filtered.size());
        } catch (Exception ex) {
            result.put("error", "Could not fetch events: " + ex.getMessage());
        }
        return result;
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
        m.put("ticketPrice", e.getTicketPrice());
        m.put("availableSeats", e.getAvailableSeats());
        m.put("totalSeats", e.getTotalSeats());
        m.put("status", e.getStatus());
        m.put("collegeName", e.getCollegeName());
        m.put("departmentName", e.getDepartmentName());
        m.put("hasCertificate", e.isHasCertificate());
        m.put("eventBanner", e.getEventBanner());
        return m;
    }

    private Long longVal(Map<String, Object> input, String key) {
        Object v = input.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String strVal(Map<String, Object> input, String key) {
        Object v = input.get(key);
        return v instanceof String s && !s.isBlank() ? s.trim() : null;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {
        return execute(input, null);
    }
}
