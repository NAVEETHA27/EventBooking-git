package com.eventbooking.ai.agent;

import com.eventbooking.security.AuthPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI Planner — decides how to handle each user message:
 *
 * 1. DIRECT     — general knowledge, greetings, platform how-to questions
 * 2. RAG_ONLY   — questions about specific events, FAQs, policies in the vector store
 * 3. TOOL_CALL  — live data needed (bookings, payments, analytics, recommendations…)
 * 4. TOOL_AND_RAG — tool output + RAG context combined for richer responses
 *
 * Decision is made in-process via keyword heuristics — no extra LLM call needed,
 * keeping latency low.
 */
@Component
@Slf4j
public class AgentPlanner {

    public AgentDecision plan(String message, AuthPrincipal principal) {
        if (!StringUtils.hasText(message)) return AgentDecision.direct();

        String q = message.toLowerCase(Locale.ROOT);
        boolean isOrganizer = principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole());
        boolean isStudent = principal != null && "USER".equalsIgnoreCase(principal.getRole());

        // ── 1. Greeting / capability question → DIRECT ────────────────────────
        if (isGreeting(q) || isCapabilityQuestion(q)) {
            return AgentDecision.direct();
        }

        // ── 2. General platform how-to / policy questions → RAG_ONLY ─────────
        if (!isOrganizer && isPolicyOrFaqQuestion(q)) {
            return AgentDecision.ragOnly();
        }

        // ── 3. For organizers: almost everything gets tools + RAG ─────────────
        List<String> tools = new ArrayList<>();

        if (isDashboardQuestion(q)) {
            tools.add("dashboardTool");
        }
        if (isOrganizer && isAnalyticsQuestion(q)) {
            tools.add("analyticsTool");
        }
        if (isEventQuestion(q)) {
            tools.add("eventTool");
        }
        if (isStudent && isBookingQuestion(q)) {
            tools.add("bookingTool");
        }
        if (isStudent && isPaymentQuestion(q)) {
            tools.add("paymentTool");
        }
        if (isCertificateQuestion(q)) {
            tools.add("certificateTool");
        }
        if (isRecommendationQuestion(q)) {
            tools.add("recommendationTool");
        }
        if (isTravelQuestion(q)) {
            tools.add("travelTool");
        }
        if (isFeedbackQuestion(q)) {
            tools.add("feedbackTool");
        }
        if (isEmailQuestion(q)) {
            tools.add("emailTool");
        }

        // ── 4. Decide strategy ────────────────────────────────────────────────
        if (tools.isEmpty()) {
            // For organizers: use RAG + direct so Gemini can still answer with context
            if (isOrganizer) return AgentDecision.ragOnly();
            return AgentDecision.ragOnly();
        }

        // Organizers always benefit from RAG enrichment for richer responses
        if (isOrganizer) {
            return AgentDecision.toolAndRag(tools);
        }

        // Students: event/recommendation queries benefit from RAG
        boolean ragBeneficial = tools.contains("eventTool")
                || tools.contains("recommendationTool")
                || tools.contains("certificateTool");

        if (ragBeneficial) {
            return AgentDecision.toolAndRag(tools);
        }
        return AgentDecision.toolCall(tools);
    }

    // ── Classifier predicates ─────────────────────────────────────────────────

    private boolean isGreeting(String q) {
        return q.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|what can you do|who are you|what are you).*");
    }

    private boolean isCapabilityQuestion(String q) {
        return q.contains("what can you") || q.contains("how can you help")
                || q.contains("what do you do") || q.contains("your features")
                || q.contains("capabilities");
    }

    private boolean isPolicyOrFaqQuestion(String q) {
        return q.contains("how to") || q.contains("how do i") || q.contains("what is")
                || q.contains("policy") || q.contains("rules") || q.contains("faq")
                || q.contains("explain") || q.contains("what happens")
                || q.contains("can i cancel") || q.contains("refund policy")
                || q.contains("terms");
    }

    private boolean isDashboardQuestion(String q) {
        return q.contains("dashboard") || q.contains("overview") || q.contains("summary")
                || q.contains("quick stats");
    }

    private boolean isAnalyticsQuestion(String q) {
        return q.contains("analytics") || q.contains("revenue") || q.contains("earnings")
                || q.contains("registrations") || q.contains("how many students")
                || q.contains("performance") || q.contains("score") || q.contains("badge")
                || q.contains("attendance rate") || q.contains("report") || q.contains("stats");
    }

    private boolean isEventQuestion(String q) {
        return q.contains("event") || q.contains("workshop") || q.contains("hackathon")
                || q.contains("seminar") || q.contains("fest") || q.contains("upcoming")
                || q.contains("schedule") || q.contains("when is") || q.contains("where is")
                || q.contains("tickets available") || q.contains("seats");
    }

    private boolean isBookingQuestion(String q) {
        return q.contains("booking") || q.contains("ticket") || q.contains("my reservation")
                || q.contains("qr code") || q.contains("booked") || q.contains("registration status");
    }

    private boolean isPaymentQuestion(String q) {
        return q.contains("payment") || q.contains("paid") || q.contains("refund")
                || q.contains("transaction") || q.contains("amount") || q.contains("charged")
                || q.contains("money back");
    }

    private boolean isCertificateQuestion(String q) {
        return q.contains("certificate") || q.contains("certification")
                || q.contains("credential") || q.contains("participation letter");
    }

    private boolean isRecommendationQuestion(String q) {
        return q.contains("recommend") || q.contains("suggest") || q.contains("what should i attend")
                || q.contains("best event") || q.contains("trending") || q.contains("popular")
                || q.contains("for me") || q.contains("free event");
    }

    private boolean isTravelQuestion(String q) {
        return q.contains("travel") || q.contains("how to reach") || q.contains("how to get to")
                || q.contains("directions") || q.contains("bus") || q.contains("train")
                || q.contains("metro") || q.contains("cab") || q.contains("hotel")
                || q.contains("parking") || q.contains("venue address");
    }

    private boolean isFeedbackQuestion(String q) {
        return q.contains("feedback") || q.contains("review") || q.contains("rating")
                || q.contains("sentiment") || q.contains("satisfaction")
                || q.contains("what did attendees") || q.contains("opinions");
    }

    private boolean isEmailQuestion(String q) {
        return q.contains("email") || q.contains("notification") || q.contains("reminder")
                || q.contains("mail") || q.contains("notify") || q.contains("send message");
    }
}
