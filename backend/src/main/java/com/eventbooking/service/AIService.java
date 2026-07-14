package com.eventbooking.service;

import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.ai.rag.RAGService;
import com.eventbooking.ai.rag.RagDocument;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.User;
import com.eventbooking.repository.*;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.ai.AIProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    private final com.eventbooking.ai.AIEngine aiEngine;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final FaqRepository faqRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final RAGService ragService;

    private final Map<String, List<Long>> requestTimes = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public AIChatResponse chat(AIChatRequest request, AuthPrincipal principal, String remoteAddress) {
        String message = sanitize(request != null ? request.getMessage() : null);
        if (!StringUtils.hasText(message)) {
            return response("Please type a question about events, bookings, payments, refunds, or organizer tools.", "fallback", defaultActions(), List.of());
        }
        if (message.length() > 1200) {
            return response("Please keep your question under 1200 characters.", "fallback", defaultActions(), List.of());
        }
        if (!allow(remoteAddress + ":" + (principal != null ? principal.getId() : "guest"))) {
            return response("You are sending messages quickly. Please wait a moment and try again.", "fallback", defaultActions(), List.of());
        }

        List<AIChatRequest.ChatMessage> history = request != null && request.getHistory() != null ? request.getHistory() : List.of();
        RAGService.RagContext ragContext = ragService.buildContext(message, principal);
        List<RagDocument> documents = ragContext.documents();
        String appContext = ragContext.formattedContext();
        String systemPrompt = ragService.guardedPrompt(ragContext);

        if (aiEngine.isAvailable() && ragContext.sufficient()) {
            try {
                String providerName = aiEngine.resolve() != null ? aiEngine.resolve().name() : "ai";
                String answer = aiEngine.complete("CHATBOT", systemPrompt, message, history, false);
                if (StringUtils.hasText(answer)) {
                    return response(answer, providerName, actionsFor(message, principal), documents);
                }
            } catch (Exception ex) {
                log.warn("AI provider failed: {}", ex.getMessage());
            }
        }
        String fallback = ragContext.sufficient()
                ? ruleBasedAnswer(message, principal, appContext)
                : "I don't have sufficient information.";
        return response(fallback, "fallback", actionsFor(message, principal), documents);
    }

    private String buildApplicationContext(String message, AuthPrincipal principal) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Public policies: Discover shows published upcoming events. Cancelled and expired tickets remain in history. Refunds are created for successful paid bookings when cancelled. QR codes are used at venue entry.\n");
        ctx.append("Upcoming events:\n");
        eventRepository.findUpcomingPublicEvents(LocalDate.now()).stream().limit(8).forEach(e ->
                ctx.append("- ").append(e.getEventName()).append(" | ").append(e.getCategory()).append(" | ")
                        .append(e.getCollegeName()).append(" | ").append(e.getEventDate()).append(" | seats ")
                        .append(e.getAvailableSeats()).append(" | price ").append(e.getTicketPrice()).append("\n"));
        ctx.append("FAQs:\n");
        faqRepository.findByActiveTrueOrderByCategoryAscQuestionAsc().stream().limit(8).forEach(f ->
                ctx.append("- ").append(f.getQuestion()).append(": ").append(f.getAnswer()).append("\n"));

        if (principal == null) {
            ctx.append("Authenticated user context: guest. Personal booking/payment/refund data is unavailable until login.\n");
            return ctx.toString();
        }

        if ("USER".equalsIgnoreCase(principal.getRole())) {
            var bookings = bookingRepository.findByUserId(principal.getId(), PageRequest.of(0, 8, Sort.by("bookedAt").descending())).getContent();
            ctx.append("Current user bookings:\n");
            bookings.forEach(b -> ctx.append("- Booking #").append(b.getId()).append(" ticket ").append(b.getTicketId())
                    .append(" | ").append(b.getEvent().getEventName()).append(" | ").append(b.getEvent().getEventDate())
                    .append(" | booking ").append(b.getBookingStatus()).append(" | ticket ").append(b.getTicketStatus())
                    .append(" | qty ").append(b.getQuantity()).append("\n"));
            ctx.append("Current user payments:\n");
            paymentRepository.findByBookingUserId(principal.getId(), PageRequest.of(0, 5, Sort.by("paidAt").descending())).forEach(p ->
                    ctx.append("- ").append(p.getTransactionId()).append(" | ").append(p.getPaymentStatus()).append(" | amount ").append(p.getAmount()).append("\n"));
            ctx.append("Current user refunds:\n");
            refundRepository.findByUserId(principal.getId()).stream().limit(5).forEach(r ->
                    ctx.append("- refund ").append(r.getRefundReference()).append(" | ").append(r.getRefundStatus()).append(" | amount ").append(r.getRefundAmount()).append("\n"));
        } else if ("ORGANIZER".equalsIgnoreCase(principal.getRole())) {
            var events = eventRepository.findByOrganizerId(principal.getId(), PageRequest.of(0, 8, Sort.by("createdAt").descending())).getContent();
            ctx.append("Current organizer events:\n");
            events.forEach(e -> ctx.append("- Event #").append(e.getId()).append(" ").append(e.getEventName()).append(" | ").append(e.getStatus())
                    .append(" | ").append(e.getEventDate()).append(" | seats ").append(e.getAvailableSeats()).append("/").append(e.getTotalSeats()).append("\n"));
            ctx.append("Admin approval queue summary: organizers can check pending/rejected approval status from My Events and admin approvals.\n");
        }
        return ctx.toString();
    }

    private String ruleBasedAnswer(String message, AuthPrincipal principal, String context) {
        String q = message.toLowerCase();
        boolean isOrganizer = principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole());

        // ── Organizer-specific answers ────────────────────────────────────
        if (isOrganizer) {
            if (q.contains("create") && q.contains("event")) {
                return "To create an event:\n\n" +
                       "1. Go to **My Events** → click **Create Event**\n" +
                       "2. Fill in event name, category, date, venue, and ticket price\n" +
                       "3. Upload a poster and authorized document\n" +
                       "4. Set food/accommodation details if applicable\n" +
                       "5. Add transportation guidance (bus stop, railway station, parking)\n" +
                       "6. Submit for approval — it will auto-publish within 10 minutes\n\n" +
                       "After publishing, your event appears on the Browse Events page for students to register.";
            }
            if (q.contains("participant") || q.contains("attendee") || q.contains("export") || q.contains("list")) {
                return "To view and export participants:\n\n" +
                       "• Go to **Attendees** from the organizer menu\n" +
                       "• View all registered participants with name, email, department, college, and ticket ID\n" +
                       "• Use the **Export CSV** button to download the full participant list\n" +
                       "• You can search by name, email, event, or department";
            }
            if (q.contains("certificate")) {
                return "Certificates are automatically generated 7 days after your event completes.\n\n" +
                       "To manage certificates:\n" +
                       "• Go to your event → **Certificates** tab\n" +
                       "• Upload a custom certificate template (PDF/JPG)\n" +
                       "• Certificates are emailed to attendees automatically\n" +
                       "• Students can view and verify certificates from their portfolio";
            }
            if (q.contains("registration") || q.contains("how many") || q.contains("registr")) {
                // Use context from RAG documents
                return "Your registration data is available in **Analytics** from the organizer dashboard.\n\n" +
                       "You can also ask me questions like:\n" +
                       "• 'How many students registered this month?'\n" +
                       "• 'Which event has the highest attendance?'\n" +
                       "• 'Show my revenue'\n\n" +
                       "The Smart Assistant (Analytics → Ask) gives real-time answers from your data.";
            }
            if (q.contains("publish") || q.contains("approval") || q.contains("approve")) {
                return "Event approval is automatic:\n\n" +
                       "• Submit your event → status becomes **Pending Approval**\n" +
                       "• It auto-approves and publishes within **10 minutes**\n" +
                       "• You'll receive a notification when it goes live\n" +
                       "• You can also manually publish from **My Events** after approval";
            }
            if (q.contains("poster") || q.contains("banner") || q.contains("image")) {
                return "To upload an event poster:\n\n" +
                       "• When creating or editing an event, use the **Event Poster** section\n" +
                       "• Supported formats: PNG, JPG, WebP (recommended: 1200×675px)\n" +
                       "• The poster appears as the event banner on the browse page and event detail page";
            }
            if (q.contains("revenue") || q.contains("earning") || q.contains("payment")) {
                return "Revenue data is in **Analytics** → **Organizer Dashboard**.\n\n" +
                       "It shows:\n" +
                       "• Total registrations and paid bookings\n" +
                       "• Revenue per event\n" +
                       "• Attendance rate and performance score\n" +
                       "• Use the prediction tool for future revenue estimates";
            }
            if (q.contains("attendance") || q.contains("check-in") || q.contains("scan")) {
                return "For event check-in:\n\n" +
                       "• Attendees show their QR ticket at the venue\n" +
                       "• Scan the QR code using the **Attendance** scanner in your organizer tools\n" +
                       "• The ticket expires immediately after scan (one-time use)\n" +
                       "• Attendance records are used for certificate eligibility";
            }
            if (q.contains("remind") || q.contains("email") || q.contains("notify")) {
                return "The platform sends automated notifications to registered participants:\n\n" +
                       "• **Booking confirmation** email after registration\n" +
                       "• **Event day reminder** sent at 6 AM on event day\n" +
                       "• **Certificate ready** email after event completion\n" +
                       "• **Payment reminders** for pending bookings\n\n" +
                       "You can also notify all attendees through the Notifications system.";
            }
            if (q.contains("predict") || q.contains("forecast") || q.contains("estimate")) {
                return "Use the **Event Prediction** tool from Analytics:\n\n" +
                       "• Predicts registration count, attendance, no-show rate\n" +
                       "• Estimates revenue and food requirements\n" +
                       "• Gives an engagement score and organizer recommendations";
            }
            // Default organizer response using context
            if (org.springframework.util.StringUtils.hasText(context)) {
                String preview = context.lines()
                        .filter(l -> l.startsWith("- [ORGANIZER"))
                        .limit(3)
                        .reduce("", (a, b) -> a + b.replaceAll("- \\[ORGANIZER_EVENT:[^]]+\\] ", "• ") + "\n");
                if (org.springframework.util.StringUtils.hasText(preview)) {
                    return "Based on your events:\n\n" + preview +
                           "\nWhat would you like to do — view participants, check analytics, create an event, or manage certificates?";
                }
            }
            return "I can help you with:\n\n" +
                   "• **Create events** — step-by-step setup\n" +
                   "• **View participants** — attendee list and CSV export\n" +
                   "• **Analytics** — registrations, revenue, performance score\n" +
                   "• **Certificates** — auto-distribution to attendees\n" +
                   "• **Event prediction** — forecast attendance and revenue\n" +
                   "• **Check-in** — QR scanner for attendance tracking\n\n" +
                   "What would you like to do?";
        }

        // ── User/guest answers ─────────────────────────────────────────────
        if ((q.contains("my booking") || q.contains("booked") || q.contains("tickets have i")) && principal == null) {
            return "Please log in to view your bookings. After login, open **My Bookings** or ask me again.";
        }
        if (q.contains("upcoming") || q.contains("available") || q.contains("weekend")) {
            return "Here are a few upcoming events I can see:\n\n" + context.lines()
                    .filter(l -> l.startsWith("- "))
                    .limit(6)
                    .reduce("", (a, b) -> a + b + "\n");
        }
        if (q.contains("refund")) return "Refunds are created after a successful paid booking is cancelled. You can track them from **Refund Status**.";
        if (q.contains("cancel")) return "To cancel, open **My Bookings**, choose an active booking, and use **Cancel**. Cancelled tickets remain in history.";
        if (q.contains("payment")) return "Paid events use online checkout. Free events confirm instantly. Payment history is in **Payments**.";
        if (q.contains("qr") || q.contains("ticket")) return "Confirmed bookings show a QR ticket on the booking detail page. Present it at the venue for entry.";
        if (q.contains("certificate")) return "Certificates are available 7 days after event completion in **My Certificates**.";
        if (q.contains("faq") || q.contains("support") || q.contains("help")) return "Open **Help / FAQ** for common answers. For issues, contact support with your booking ID.";
        return "I can help with upcoming events, bookings, refunds, cancellations, payments, QR tickets, organizer tools, and certificates.\n\nWhat would you like to do?";
    }

    private List<AIChatResponse.QuickAction> actionsFor(String message, AuthPrincipal principal) {
        String q = message.toLowerCase();
        List<AIChatResponse.QuickAction> actions = new ArrayList<>(defaultActions());
        if (q.contains("refund")) actions.add(action("Refund Status", "/refunds"));
        if (q.contains("booking") || q.contains("ticket")) actions.add(action("My Bookings", "/bookings"));
        if (q.contains("organizer")) actions.add(action("Organizer Dashboard", "/organizer/dashboard"));
        if (principal == null) actions.add(action("Login", "/login"));
        return actions.stream().distinct().limit(8).toList();
    }

    private List<AIChatResponse.QuickAction> defaultActions() {
        return List.of(
                action("Book an Event", "/events"),
                action("Upcoming Events", "/events"),
                action("FAQ", "/help"),
                action("Contact Support", "/help")
        );
    }

    private AIChatResponse.QuickAction action(String label, String path) {
        return AIChatResponse.QuickAction.builder().label(label).path(path).build();
    }

    private AIChatResponse response(String answer, String provider, List<AIChatResponse.QuickAction> actions, List<RagDocument> documents) {
        return AIChatResponse.builder()
                .answer(answer)
                .provider(provider)
                .timestamp(LocalDateTime.now())
                .actions(actions)
                .sources(documents.stream()
                        .limit(6)
                        .map(document -> AIChatResponse.Source.builder()
                                .type(document.sourceType())
                                .id(document.sourceId())
                                .title(document.title())
                                .build())
                        .toList())
                .build();
    }

    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        List<Long> times = requestTimes.computeIfAbsent(key, ignored -> new ArrayList<>());
        synchronized (times) {
            times.removeIf(t -> now - t > 60_000);
            if (times.size() >= 20) return false;
            times.add(now);
            return true;
        }
    }

    private String sanitize(String message) {
        if (message == null) return "";
        return message.replaceAll("(?i)(api[_ -]?key|password|token)\\s*[:=]\\s*\\S+", "$1=[redacted]").trim();
    }

    // ── NLP Search ────────────────────────────────────────────────────────────

    /**
     * Parse a natural language query and return matching events.
     * Supports: price filters, keywords, categories, date keywords,
     * event type, food/accommodation, location.
     * Examples: "AI workshop under 500", "free hackathon near Chennai", "tomorrow online"
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> nlpSearch(String query, Long userId) {
        if (!StringUtils.hasText(query)) return List.of();
        List<Event> allUpcoming = eventRepository.findUpcomingPublicEvents(LocalDate.now());

        // ── Parse query heuristically (no LLM needed for basic cases) ─────
        String q = query.toLowerCase(java.util.Locale.ROOT).trim();

        // 1. Price extraction: "under 500", "below ₹500", "less than 500", "under rs.500"
        java.math.BigDecimal maxPrice = null;
        java.util.regex.Matcher priceMatcher = java.util.regex.Pattern
                .compile("(?:under|below|less\\s+than|<|upto?|within)\\s*[₹rs\\.]*\\s*(\\d+(?:[.,]\\d+)?)")
                .matcher(q);
        if (priceMatcher.find()) {
            try { maxPrice = new java.math.BigDecimal(priceMatcher.group(1).replace(",", "")); }
            catch (Exception ignored) {}
        }

        // 2. Category/keyword normalization
        boolean wantHackathon     = q.contains("hackathon");
        boolean wantWorkshop      = q.contains("workshop");
        boolean wantSeminar       = q.contains("seminar");
        boolean wantTechnical     = q.contains("technical") || q.contains("tech");
        boolean wantCultural      = q.contains("cultural") || q.contains("culture");
        boolean wantSports        = q.contains("sport");
        boolean wantAi            = q.contains(" ai ") || q.startsWith("ai ") || q.contains("artificial intelligence") || q.contains("machine learning") || q.contains("ml ");
        boolean wantFree          = q.contains("free");
        boolean wantOnline        = q.contains("online");
        boolean wantOffline       = q.contains("offline") || q.contains("in-person") || q.contains("in person");
        boolean wantFood          = q.contains("food") || q.contains("meal") || q.contains("lunch") || q.contains("dinner");
        boolean wantAccommodation = q.contains("accommodation") || q.contains("hostel") || q.contains("stay") || q.contains("hotel");

        // 3. Date keywords
        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate weekEnd  = today.plusDays(7);
        boolean wantToday     = q.contains("today");
        boolean wantTomorrow  = q.contains("tomorrow");
        boolean wantThisWeek  = q.contains("this week") || q.contains("week");
        boolean wantWeekend   = q.contains("weekend");
        boolean wantNextWeek  = q.contains("next week");

        // 4. Location keyword (strip known price/date words first)
        String locationHint = q
                .replaceAll("(?:under|below|less\\s+than|upto?|within)\\s*[₹rs\\.]*\\s*\\d+", "")
                .replaceAll("(?:free|online|offline|hackathon|workshop|seminar|technical|today|tomorrow|this week|next week|weekend|food|accommodation)", "")
                .replaceAll("\\s+", " ").trim();

        final java.math.BigDecimal finalMaxPrice = maxPrice;

        // AI-assisted parsing for complex queries (optional enrichment)
        if (aiEngine.isAvailable() && finalMaxPrice == null && !wantHackathon && !wantWorkshop && !wantFree) {
            try {
                String prompt = """
                        Parse this event search query into JSON with exactly these keys:
                        {"category":null,"location":null,"dateKeyword":null,"maxPrice":null,"eventType":null,"keyword":null}
                        Rules: category can be HACKATHON/WORKSHOP/SEMINAR/CULTURAL/SPORTS/null,
                        dateKeyword: today/tomorrow/this_week/weekend/next_week/null,
                        maxPrice: number or null, eventType: ONLINE/OFFLINE/HYBRID/null.
                        Query: "%s"
                        Return only valid flat JSON, no markdown, no explanation.
                        """.formatted(query);
                String json = aiEngine.complete("NLP_SEARCH", "You are a search query parser. Return only JSON.", prompt);
                if (json != null && json.contains("{")) {
                    // Try to extract maxPrice from AI parse as well
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"maxPrice\"\\s*:\\s*(\\d+)").matcher(json);
                    if (m.find() && finalMaxPrice == null) {
                        // AI found a price — use it silently
                    }
                }
            } catch (Exception ex) {
                log.debug("NLP AI parse skipped: {}", ex.getMessage());
            }
        }

        return allUpcoming.stream().filter(e -> {
            // Price filter — most important for queries like "under 500"
            if (finalMaxPrice != null && e.getTicketPrice().compareTo(finalMaxPrice) > 0) return false;

            // Free events
            if (wantFree && e.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) != 0) return false;

            // Event type
            if (wantOnline  && !"ONLINE".equalsIgnoreCase(e.getEventType()))  return false;
            if (wantOffline && !"OFFLINE".equalsIgnoreCase(e.getEventType())) return false;

            // Food / accommodation
            if (wantFood          && !Boolean.TRUE.equals(e.getFoodProvided()))          return false;
            if (wantAccommodation && !Boolean.TRUE.equals(e.getAccommodationProvided())) return false;

            // Date filters
            if (wantToday    && !e.getEventDate().isEqual(today))    return false;
            if (wantTomorrow && !e.getEventDate().isEqual(tomorrow))  return false;
            if (wantThisWeek && (e.getEventDate().isBefore(today) || e.getEventDate().isAfter(weekEnd))) return false;
            if (wantNextWeek) {
                LocalDate nwStart = today.plusDays(7);
                LocalDate nwEnd   = today.plusDays(14);
                if (e.getEventDate().isBefore(nwStart) || e.getEventDate().isAfter(nwEnd)) return false;
            }
            if (wantWeekend) {
                java.time.DayOfWeek dow = e.getEventDate().getDayOfWeek();
                if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) return false;
            }

            // Category keywords
            String cat = e.getCategory() != null ? e.getCategory().toLowerCase() : "";
            String tags = e.getTags() != null ? e.getTags().toLowerCase() : "";
            String name = e.getEventName().toLowerCase();
            String desc = e.getDescription() != null ? e.getDescription().toLowerCase() : "";

            if (wantHackathon && !cat.contains("hackathon") && !name.contains("hackathon") && !tags.contains("hackathon")) return false;
            if (wantWorkshop  && !cat.contains("workshop")  && !name.contains("workshop")  && !tags.contains("workshop"))  return false;
            if (wantSeminar   && !cat.contains("seminar")   && !name.contains("seminar")   && !tags.contains("seminar"))   return false;
            if (wantTechnical && !cat.contains("tech") && !name.contains("tech") && !tags.contains("tech") && !desc.contains("tech")) return false;
            if (wantCultural  && !cat.contains("cultural")  && !name.contains("cultural"))  return false;
            if (wantSports    && !cat.contains("sport")     && !name.contains("sport"))     return false;
            if (wantAi        && !name.contains("ai") && !tags.contains("ai") && !tags.contains("machine learning") && !desc.contains("artificial intelligence")) return false;

            // Location keyword
            if (StringUtils.hasText(locationHint) && locationHint.length() > 2) {
                String loc = e.getLocation() != null ? e.getLocation().toLowerCase() : "";
                String venue = e.getVenueName() != null ? e.getVenueName().toLowerCase() : "";
                String college = e.getCollegeName() != null ? e.getCollegeName().toLowerCase() : "";
                if (!loc.contains(locationHint) && !venue.contains(locationHint) && !college.contains(locationHint)) {
                    // Only apply location filter if no other filter matched — prevents over-filtering
                    if (finalMaxPrice == null && !wantHackathon && !wantWorkshop && !wantFree && !wantOnline) return false;
                }
            }

            return true;
        }).sorted((a, b) -> {
            // Sort: earlier events first, then by seats filled ratio (popularity)
            int dateCmp = a.getEventDate().compareTo(b.getEventDate());
            if (dateCmp != 0) return dateCmp;
            double aFill = a.getTotalSeats() > 0 ? (double)(a.getTotalSeats() - a.getAvailableSeats()) / a.getTotalSeats() : 0;
            double bFill = b.getTotalSeats() > 0 ? (double)(b.getTotalSeats() - b.getAvailableSeats()) / b.getTotalSeats() : 0;
            return Double.compare(bFill, aFill);
        }).limit(12).map(e -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("eventName", e.getEventName());
            m.put("category", e.getCategory());
            m.put("eventType", e.getEventType());
            m.put("eventDate", e.getEventDate());
            m.put("location", e.getLocation());
            m.put("ticketPrice", e.getTicketPrice());
            m.put("availableSeats", e.getAvailableSeats());
            m.put("eventBanner", e.getEventBanner());
            m.put("foodProvided", Boolean.TRUE.equals(e.getFoodProvided()));
            m.put("accommodationProvided", Boolean.TRUE.equals(e.getAccommodationProvided()));
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    // ── Event Summary ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> generateEventSummary(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new RuntimeException("Event not found"));
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("eventName", event.getEventName());
        summary.put("category", event.getCategory());
        summary.put("eventType", event.getEventType());
        summary.put("eventDate", event.getEventDate());

        if (aiEngine.isAvailable()) {
            try {
                String prompt = """
                        Generate a structured event summary for:
                        Event: %s
                        Category: %s
                        Description: %s
                        Type: %s
                        Date: %s
                        Provide sections:
                        ## Who Should Attend
                        ## Prerequisites
                        ## Learning Outcomes
                        ## Career Benefits
                        ## Required Skills
                        Keep each section to 2-3 bullet points.
                        """.formatted(event.getEventName(), event.getCategory(),
                        event.getDescription() != null ? event.getDescription() : "N/A",
                        event.getEventType(), event.getEventDate());
                summary.put("aiSummary", aiEngine.complete("EVENT_SUMMARY",
                        "You are an event content expert.", prompt));
            } catch (Exception ex) {
                summary.put("aiSummary", null);
            }
        }
        return summary;
    }

    // ── Description Generator ─────────────────────────────────────────────────

    public Map<String, Object> generateEventDescription(String prompt) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (!aiEngine.isAvailable() || !StringUtils.hasText(prompt)) {
            result.put("description", "AI generation unavailable. Please configure an AI provider.");
            return result;
        }
        try {
            String fullPrompt = """
                    For this event idea: "%s"
                    Generate:
                    ## Description (2 paragraphs)
                    ## Agenda (5-6 bullet items with times)
                    ## Rules (3-4 bullet points)
                    ## Learning Outcomes (4 bullet points)
                    ## FAQs (3 Q&A pairs)
                    ## Social Media Caption (1 Instagram-ready caption)
                    ## Invitation Email (professional 5-sentence invitation)
                    """.formatted(prompt);
            String generated = aiEngine.complete("DESCRIPTION_GENERATOR",
                    "You are a professional event content writer.", fullPrompt);
            result.put("generated", generated);
            result.put("prompt", prompt);
        } catch (Exception ex) {
            result.put("error", "Generation failed: " + ex.getMessage());
        }
        return result;
    }

    // ── Career Guidance ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String generateCareerGuidance(Long userId) {
        var bookings = bookingRepository.findByUserId(userId,
                org.springframework.data.domain.PageRequest.of(0, 50,
                        org.springframework.data.domain.Sort.by("bookedAt").descending())).getContent();
        if (bookings.isEmpty()) return "Register for events to get personalized career guidance!";

        String categories = bookings.stream()
                .map(b -> b.getEvent().getCategory())
                .distinct().limit(5)
                .collect(java.util.stream.Collectors.joining(", "));

        if (!aiEngine.isAvailable()) return "Based on your participation in " + categories + " events, explore related career paths and certifications.";
        try {
            String prompt = """
                    Based on a student's participation in these event categories: %s
                    Provide personalized career guidance:
                    ## Recommended Career Paths
                    ## Suggested Certifications
                    ## Recommended Courses
                    ## Internship Focus Areas
                    ## Learning Roadmap (3-month plan)
                    Keep it concise and actionable.
                    """.formatted(categories);
            return aiEngine.complete("CAREER_GUIDANCE", "You are a student career counselor.", prompt);
        } catch (Exception ex) {
            return "Explore careers in: " + categories + ". Consider certifications in related fields.";
        }
    }

    // ── Travel Assistant ──────────────────────────────────────────────────────

    /**
     * Generate travel information for reaching an event venue.
     * Uses AI to produce transport options, nearby hotels, weather forecast etc.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateTravelInfo(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("eventName", event.getEventName());
        result.put("venueName", event.getVenueName());
        result.put("location", event.getLocation());
        result.put("eventDate", event.getEventDate());
        result.put("googleMapsUrl", event.getGoogleMapsUrl());

        // Fallback structured data even without AI
        result.put("transportOptions", List.of(
                Map.of("mode", "Bus",    "description", "Check local city bus routes to the venue area."),
                Map.of("mode", "Train",  "description", "Look up the nearest railway station on IRCTC / Google Maps."),
                Map.of("mode", "Metro",  "description", "Check metro connectivity via official metro app."),
                Map.of("mode", "Cab",    "description", "Book via Ola / Uber — estimated fare varies by distance."),
                Map.of("mode", "Walking","description", "Use Google Maps walking directions from the nearest landmark.")
        ));
        result.put("parkingAvailable", "Contact the organizer for venue-specific parking details.");
        result.put("nearbyHotels",      "Search hotels near the venue on MakeMyTrip / Booking.com.");
        result.put("nearbyRestaurants", "Use Google Maps or Zomato to find restaurants near the venue.");
        result.put("weatherNote",       "Check weather.com or Google Weather for forecast on event day.");

        // AI enhancement — richer, venue-specific travel guide
        if (aiEngine.isAvailable()) {
            try {
                String venue   = event.getVenueName()   != null ? event.getVenueName()   : "";
                String city    = event.getLocation()    != null ? event.getLocation()    : "";
                String date    = event.getEventDate()   != null ? event.getEventDate().toString() : "";
                String prompt  = """
                        Generate a comprehensive travel guide for attending this event:
                        Event: %s
                        Venue: %s
                        City/Location: %s
                        Date: %s

                        Provide detailed information in these sections:
                        ## Nearest Bus Stand & Bus Numbers
                        ## Nearest Railway Station & Train Routes
                        ## Metro Connectivity
                        ## Airport Distance & Route
                        ## Estimated Travel Times (from city center)
                        ## Cab Fare Estimate (Ola/Uber)
                        ## Parking Availability
                        ## Nearby Hotels (budget and premium options)
                        ## Nearby Restaurants
                        ## Weather Forecast Tips for this date
                        ## Walking Directions from nearest landmark

                        Be specific and practical. Mention real transport options if known.
                        """.formatted(event.getEventName(), venue, city, date);

                String aiGuide = aiEngine.complete("TRAVEL_ASSISTANT",
                        "You are a knowledgeable local travel assistant helping event attendees reach venues.", prompt);
                result.put("aiTravelGuide", aiGuide);
            } catch (Exception ex) {
                log.warn("AI travel guide generation failed for event {}: {}", eventId, ex.getMessage());
                result.put("aiTravelGuide", null);
            }
        }
        return result;
    }
}
