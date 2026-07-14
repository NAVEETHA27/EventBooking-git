package com.eventbooking.service;

import com.eventbooking.dto.request.AgentToolRequest;
import com.eventbooking.dto.request.EventSearchRequest;
import com.eventbooking.dto.response.AgentToolResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ParticipantRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventGptToolService {
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventService eventService;
    private final OrganizerAnalyticsService organizerAnalyticsService;
    private final RecommendationService recommendationService;
    private final CertificateService certificateService;
    private final EventAssetService eventAssetService;
    private final AIService aiService;

    @Transactional(readOnly = true)
    public AgentToolResponse invoke(AgentToolRequest request, AuthPrincipal principal) {
        String toolName = request.getToolName() != null && !request.getToolName().isBlank()
                ? request.getToolName()
                : decideTool(request.getPrompt());
        return switch (toolName) {
            case "createEvent" -> createEvent(request, principal);
            case "updateEvent" -> updateEvent(request, principal);
            case "deleteEvent" -> deleteEvent(request, principal);
            case "sendEmail" -> sendEmail(request, principal);
            case "getParticipants" -> getParticipants(request, principal);
            case "generateCertificate" -> generateCertificate(request, principal);
            case "generatePoster" -> generatePoster(request, principal);
            case "exportExcel" -> exportExcel(request, principal);
            case "getAnalytics" -> getAnalytics(principal);
            case "recommendPrice" -> recommendPrice(request);
            case "predictAttendance" -> predictAttendance(request, principal);
            case "summarizeFeedback" -> summarizeFeedback(request);
            case "recommendEvents" -> recommendEvents(principal);
            case "generateDescription" -> generateDescription(request);
            case "searchEvents" -> searchEvents(request, principal);
            default -> response(toolName, "unsupported", false, "Tool is not available.", Map.of());
        };
    }

    public List<String> availableTools() {
        return List.of("createEvent", "updateEvent", "deleteEvent", "sendEmail", "getParticipants",
                "generateCertificate", "generatePoster", "exportExcel", "getAnalytics", "recommendPrice",
                "predictAttendance", "summarizeFeedback", "recommendEvents", "generateDescription", "searchEvents");
    }

    private AgentToolResponse createEvent(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("createEvent", "denied", false, "Organizer login is required to create events.", Map.of());
        }
        return response("createEvent", "confirmation_required", true,
                "Create events through the existing organizer event API after reviewing the generated draft.",
                Map.of("draft", request.getArguments()));
    }

    private AgentToolResponse updateEvent(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("updateEvent", "denied", false, "Organizer login is required to update events.", Map.of());
        }
        return response("updateEvent", "confirmation_required", true,
                "Event updates require the existing organizer update API so bookings and approvals remain safe.",
                Map.of("proposedChanges", request.getArguments()));
    }

    private AgentToolResponse deleteEvent(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("deleteEvent", "denied", false, "Organizer login is required to cancel events.", Map.of());
        }
        return response("deleteEvent", "confirmation_required", true,
                "Deleting maps to event cancellation and requires explicit confirmation in the existing event API.",
                request.getArguments());
    }

    private AgentToolResponse sendEmail(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal) && !isAdmin(principal)) {
            return response("sendEmail", "denied", false, "Organizer or admin login is required to send emails.", Map.of());
        }
        return response("sendEmail", "confirmation_required", true,
                "Email delivery requires recipient review before sending.", request.getArguments());
    }

    private AgentToolResponse getParticipants(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("getParticipants", "denied", false, "Organizer login is required to view participants.", Map.of());
        }
        Long eventId = longArg(request, "eventId");
        List<Map<String, Object>> participants = participantRepository.findByEventOrganizerIdWithDetails(principal.getId()).stream()
                .filter(participant -> eventId == null || participant.getEvent().getId().equals(eventId))
                .limit(200)
                .map(participant -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", participant.getId());
                    row.put("name", participant.getName());
                    row.put("email", participant.getEmail());
                    row.put("department", participant.getDepartment());
                    row.put("college", participant.getCollege());
                    row.put("eventId", participant.getEvent().getId());
                    row.put("eventName", participant.getEvent().getEventName());
                    return row;
                })
                .toList();
        return response("getParticipants", "success", false, "Participants retrieved.", Map.of("participants", participants));
    }

    private AgentToolResponse generateCertificate(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("generateCertificate", "denied", false, "Organizer login is required to generate certificates.", Map.of());
        }
        Long eventId = longArg(request, "eventId");
        if (eventId == null) {
            return response("generateCertificate", "needs_input", false, "eventId is required.", Map.of());
        }
        certificateService.generateCertificatesForEvent(eventId);
        return response("generateCertificate", "queued", false, "Certificate generation has been queued.", Map.of("eventId", eventId));
    }

    private AgentToolResponse generatePoster(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("generatePoster", "denied", false, "Organizer login is required to generate posters.", Map.of());
        }
        Long eventId = longArg(request, "eventId");
        if (eventId == null) {
            return response("generatePoster", "needs_input", false, "eventId is required.", Map.of());
        }
        try {
            String url = eventAssetService.generatePoster(eventId, principal.getId());
            return response("generatePoster", "success", false, "Poster generated.", Map.of("posterUrl", url));
        } catch (Exception ex) {
            return response("generatePoster", "failed", false, ex.getMessage(), Map.of("eventId", eventId));
        }
    }

    private AgentToolResponse exportExcel(AgentToolRequest request, AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("exportExcel", "denied", false, "Organizer login is required to export participants.", Map.of());
        }
        Long eventId = longArg(request, "eventId");
        if (eventId == null) {
            return response("exportExcel", "needs_input", false, "eventId is required.", Map.of());
        }
        return response("exportExcel", "success", false,
                "Participant export is ready from the organizer attendees export endpoint.",
                Map.of("downloadPath", "/api/organizer/events/" + eventId + "/participants/export"));
    }

    private AgentToolResponse getAnalytics(AuthPrincipal principal) {
        if (!isOrganizer(principal)) {
            return response("getAnalytics", "denied", false, "Organizer login is required to view analytics.", Map.of());
        }
        return response("getAnalytics", "success", false, "Analytics retrieved.",
                organizerAnalyticsService.getOrganizerAnalytics(principal.getId()));
    }

    private AgentToolResponse recommendPrice(AgentToolRequest request) {
        int capacity = intArg(request, "capacity", 100);
        int base = capacity > 250 ? 399 : capacity > 100 ? 299 : 199;
        return response("recommendPrice", "success", false, "Recommended price calculated.",
                Map.of("recommendedPrice", BigDecimal.valueOf(base), "currency", "INR"));
    }

    private AgentToolResponse predictAttendance(AgentToolRequest request, AuthPrincipal principal) {
        Long eventId = longArg(request, "eventId");
        if (eventId != null && isOrganizer(principal)) {
            return response("predictAttendance", "success", false, "Prediction generated.",
                    organizerAnalyticsService.predictEventOutcomes(eventId, principal.getId()));
        }
        int registrations = intArg(request, "registrations", 0);
        return response("predictAttendance", "success", false, "Prediction generated.",
                Map.of("predictedAttendance", Math.round(registrations * 0.85), "noShowRate", "15%"));
    }

    private AgentToolResponse summarizeFeedback(AgentToolRequest request) {
        return response("summarizeFeedback", "success", false,
                "Feedback summarization will use approved reviews from the feedback AI phase.",
                Map.of("summary", "Feedback data is not supplied in this request."));
    }

    private AgentToolResponse recommendEvents(AuthPrincipal principal) {
        Long userId = principal != null && "USER".equalsIgnoreCase(principal.getRole()) ? principal.getId() : null;
        return response("recommendEvents", "success", false, "Recommendations retrieved.",
                Map.of("sections", recommendationService.getDiscoverRecommendations(userId, null, null)));
    }

    private AgentToolResponse generateDescription(AgentToolRequest request) {
        return response("generateDescription", "success", false, "Description generated.",
                aiService.generateEventDescription(request.getPrompt()));
    }

    private AgentToolResponse searchEvents(AgentToolRequest request, AuthPrincipal principal) {
        EventSearchRequest search = EventSearchRequest.builder()
                .keyword(request.getPrompt())
                .page(0)
                .size(10)
                .build();
        Long userId = principal != null ? principal.getId() : null;
        return response("searchEvents", "success", false, "Events retrieved.",
                Map.of("events", eventService.search(search, userId).getContent()));
    }

    private String decideTool(String prompt) {
        String q = prompt == null ? "" : prompt.toLowerCase();
        if (q.contains("create") && q.contains("event")) return "createEvent";
        if (q.contains("update") || q.contains("edit")) return "updateEvent";
        if (q.contains("delete") || q.contains("cancel event")) return "deleteEvent";
        if (q.contains("email") || q.contains("reminder")) return "sendEmail";
        if (q.contains("participant") || q.contains("registered")) return "getParticipants";
        if (q.contains("certificate")) return "generateCertificate";
        if (q.contains("poster")) return "generatePoster";
        if (q.contains("excel") || q.contains("export")) return "exportExcel";
        if (q.contains("analytics") || q.contains("revenue")) return "getAnalytics";
        if (q.contains("price")) return "recommendPrice";
        if (q.contains("predict") || q.contains("attendance")) return "predictAttendance";
        if (q.contains("feedback") || q.contains("review")) return "summarizeFeedback";
        if (q.contains("recommend")) return "recommendEvents";
        if (q.contains("description")) return "generateDescription";
        return "searchEvents";
    }

    private AgentToolResponse response(String toolName, String status, boolean requiresConfirmation, String message, Map<String, Object> data) {
        return AgentToolResponse.builder()
                .toolName(toolName)
                .status(status)
                .requiresConfirmation(requiresConfirmation)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    private boolean isOrganizer(AuthPrincipal principal) {
        return principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole());
    }

    private boolean isAdmin(AuthPrincipal principal) {
        return principal != null && "ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private Long longArg(AgentToolRequest request, String key) {
        Object value = request.getArguments().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int intArg(AgentToolRequest request, String key, int fallback) {
        Object value = request.getArguments().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
