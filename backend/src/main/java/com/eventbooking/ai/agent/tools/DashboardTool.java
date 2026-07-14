package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.OrganizerAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardTool implements AgentTool {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final OrganizerAnalyticsService analyticsService;

    @Override
    public String name() { return "dashboardTool"; }

    @Override
    public String description() {
        return "Returns a quick dashboard summary: organizer sees event count + top metrics; students see upcoming events count and recent activity.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole())) {
                // Organizer dashboard snapshot
                var events = eventRepository.findByOrganizerId(
                        principal.getId(), PageRequest.of(0, 50, Sort.by("createdAt").descending())).getContent();
                long published = events.stream().filter(e -> e.getStatus().name().equals("PUBLISHED")).count();
                long completed = events.stream().filter(e ->
                        e.getStatus().name().equals("COMPLETED") || e.getStatus().name().equals("EXPIRED")).count();
                long pending = events.stream().filter(e -> e.getStatus().name().equals("PENDING_APPROVAL")).count();
                long totalSeats = events.stream().mapToLong(e -> e.getTotalSeats()).sum();
                long availableSeats = events.stream().mapToLong(e -> e.getAvailableSeats()).sum();
                long totalRegistrations = totalSeats - availableSeats;

                result.put("totalEvents", events.size());
                result.put("publishedEvents", published);
                result.put("completedEvents", completed);
                result.put("pendingApproval", pending);
                result.put("totalRegistrations", totalRegistrations);
                result.put("recentEvents", events.stream().limit(5).map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("eventName", e.getEventName());
                    m.put("status", e.getStatus());
                    m.put("eventDate", e.getEventDate());
                    m.put("registrations", e.getTotalSeats() - e.getAvailableSeats());
                    return m;
                }).collect(Collectors.toList()));
            } else {
                // Student/guest dashboard
                long upcomingCount = eventRepository.findUpcomingPublicEvents(LocalDate.now()).size();
                result.put("upcomingEventsCount", upcomingCount);
                if (principal != null) {
                    long myBookings = bookingRepository.findByUserId(
                            principal.getId(), PageRequest.of(0, 1, Sort.by("bookedAt").descending()))
                            .getTotalElements();
                    result.put("myTotalBookings", myBookings);
                }
                result.put("quickLinks", java.util.List.of(
                        Map.of("label", "Browse Events", "path", "/events"),
                        Map.of("label", "My Bookings", "path", "/bookings"),
                        Map.of("label", "My Certificates", "path", "/certificates")
                ));
            }
        } catch (Exception ex) {
            result.put("error", "Could not load dashboard: " + ex.getMessage());
        }
        return result;
    }
}
