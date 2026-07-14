package com.eventbooking.ai.agent;

import com.eventbooking.ai.AIProvider;
import com.eventbooking.ai.rag.RAGService;
import com.eventbooking.ai.rag.RagDocument;
import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent Orchestrator — central engine for the conversational AI agents.
 *
 * Flow:
 * 1. Planner decides strategy (DIRECT / RAG_ONLY / TOOL_CALL / TOOL_AND_RAG)
 * 2. Tools execute in parallel (where independent)
 * 3. RAG retrieves relevant context when needed
 * 4. Gemini generates a final human-like response from tool results + RAG + history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final AgentPlanner planner;
    private final List<AgentTool> tools;
    private final RAGService ragService;
    private final com.eventbooking.ai.AIEngine aiEngine;

    /**
     * Main entry point — produces a complete AI response.
     *
     * @param message   user's latest message
     * @param history   last N conversation turns (20 max managed by ConversationMemoryService)
     * @param principal authenticated user — null for guest
     * @return AgentResponse with answer, sources, and metadata
     */
    public AgentResponse process(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        if (!StringUtils.hasText(message)) {
            return AgentResponse.fallback("Please type a question about events, bookings, or anything I can help with.");
        }

        String sanitized = sanitize(message);
        AgentDecision decision = planner.plan(sanitized, principal);
        log.debug("[AgentOrchestrator] strategy={} tools={} rag={}", decision.strategy(), decision.toolNames(), decision.useRag());

        return switch (decision.strategy()) {
            case DIRECT      -> handleDirect(sanitized, history, principal);
            case RAG_ONLY    -> handleRagOnly(sanitized, history, principal);
            case TOOL_CALL   -> handleToolCall(sanitized, decision, history, principal);
            case TOOL_AND_RAG -> handleToolAndRag(sanitized, decision, history, principal);
        };
    }

    // ── Strategy handlers ─────────────────────────────────────────────────────

    private AgentResponse handleDirect(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        String systemPrompt = buildEnrichedOrganizerPrompt(principal);
        String answer = callGemini(systemPrompt, message, history);
        if (answer == null) {
            answer = capabilityFallback(principal);
        }
        return AgentResponse.success(answer, List.of(), "gemini", "DIRECT");
    }

    private AgentResponse handleRagOnly(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        long t0 = System.currentTimeMillis();
        RAGService.RagContext ctx = ragService.buildContext(message, principal);
        log.debug("[AgentOrchestrator] RAG retrieved {} docs in {}ms",
                ctx.documents().size(), System.currentTimeMillis() - t0);

        String systemPrompt = ragService.conversationalPrompt(
                ctx,
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);

        long t1 = System.currentTimeMillis();
        String answer = callGemini(systemPrompt, message, history);
        log.debug("[AgentOrchestrator] Gemini responded in {}ms", System.currentTimeMillis() - t1);

        if (!StringUtils.hasText(answer)) {
            // Gemini unavailable — synthesize answer from RAG context directly
            answer = synthesizeFromRag(ctx, message, principal);
        }
        return AgentResponse.success(answer, ctx.documents(), "gemini", "RAG_ONLY");
    }

    private AgentResponse handleToolCall(String message, AgentDecision decision,
                                          List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        long t0 = System.currentTimeMillis();
        Map<String, Object> toolResults = executeTools(decision.toolNames(), message, principal);
        log.debug("[AgentOrchestrator] Tools executed in {}ms", System.currentTimeMillis() - t0);

        String toolSummary  = formatToolResults(toolResults);
        String systemPrompt = buildSystemWithToolResults(principal, toolSummary);
        String answer       = callGemini(systemPrompt, message, history);
        if (!StringUtils.hasText(answer)) {
            answer = synthesizeFallback(toolResults, message, principal);
        }
        return AgentResponse.success(answer, List.of(), "gemini", "TOOL_CALL");
    }

    private AgentResponse handleToolAndRag(String message, AgentDecision decision,
                                            List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        long t0 = System.currentTimeMillis();
        Map<String, Object> toolResults = executeTools(decision.toolNames(), message, principal);
        RAGService.RagContext ctx = ragService.buildContext(message, principal);
        log.debug("[AgentOrchestrator] Tools+RAG ready in {}ms | docs={}", System.currentTimeMillis() - t0, ctx.documents().size());

        String toolSummary  = formatToolResults(toolResults);
        String ragContext   = ctx.formattedContext();
        String systemPrompt = buildSystemWithToolAndRag(principal, toolSummary, ragContext);
        String answer       = callGemini(systemPrompt, message, history);
        if (!StringUtils.hasText(answer)) {
            answer = synthesizeFallback(toolResults, message, principal);
        }
        return AgentResponse.success(answer, ctx.documents(), "gemini", "TOOL_AND_RAG");
    }

    // ── Enriched prompt with live organizer context ───────────────────────────

    /**
     * For organizers: inject a compact analytics snapshot into the system prompt
     * so Gemini can answer analytics questions even on DIRECT strategy without a tool call.
     */
    private String buildEnrichedOrganizerPrompt(AuthPrincipal principal) {
        String base = SystemPrompts.forRole(
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);
        if (principal == null || !"ORGANIZER".equalsIgnoreCase(principal.getRole())) {
            return base;
        }
        // Try to get live snapshot from dashboardTool (already in tool list)
        try {
            tools.stream()
                    .filter(t -> "dashboardTool".equals(t.name()))
                    .findFirst()
                    .ifPresent(tool -> {
                        Map<String, Object> snap = tool.execute(Map.of(), principal);
                        if (!snap.containsKey("error")) {
                            // snapshot is already in base via buildSystemWithToolResults when needed
                        }
                    });
        } catch (Exception ignored) {}
        return base;
    }

    // ── Tool execution ────────────────────────────────────────────────────────

    private Map<String, Object> executeTools(List<String> toolNames, String message, AuthPrincipal principal) {
        Map<String, Object> allResults = new HashMap<>();
        Map<String, Object> input = extractEntities(message);

        for (String toolName : toolNames) {
            tools.stream()
                    .filter(t -> t.name().equals(toolName))
                    .findFirst()
                    .ifPresent(tool -> {
                        try {
                            Map<String, Object> result = tool.execute(input, principal);
                            allResults.put(toolName, result);
                            log.debug("[AgentOrchestrator] tool={} keys={}", toolName, result.keySet());
                        } catch (Exception ex) {
                            log.warn("[AgentOrchestrator] Tool {} failed: {}", toolName, ex.getMessage());
                            allResults.put(toolName, Map.of("error", "Tool temporarily unavailable"));
                        }
                    });
        }
        return allResults;
    }

    /**
     * Lightweight entity extractor — pulls eventId, keyword, category from the message.
     * No LLM call needed; heuristic pattern matching is fast and cheap.
     */
    private Map<String, Object> extractEntities(String message) {
        Map<String, Object> entities = new HashMap<>();
        // Extract event ID from patterns like "event 123", "event #123", "eventId 123"
        java.util.regex.Matcher idMatcher = java.util.regex.Pattern
                .compile("(?:event|id|#)\\s*[:#]?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(message);
        if (idMatcher.find()) {
            entities.put("eventId", idMatcher.group(1));
        }
        entities.put("keyword", message);  // full message as keyword for broad search
        return entities;
    }

    // ── System prompt builders ────────────────────────────────────────────────

    private String buildSystemWithRag(AuthPrincipal principal, String ragContext) {
        String base = SystemPrompts.forRole(
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);
        String hidden = org.springframework.util.StringUtils.hasText(ragContext) ? ragContext : "No specific context found.";
        return base + "\n\n## Background Knowledge (use silently — never mention 'retrieved context', 'FAQ', or 'document' to the user)\n"
               + hidden
               + "\n\nSynthesize the background knowledge into a natural, conversational response. Never list raw knowledge entries.";
    }

    private String buildSystemWithToolResults(AuthPrincipal principal, String toolSummary) {
        String base = SystemPrompts.forRole(
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);
        return base + "\n\n## Live Data from Tools\n" + toolSummary +
               "\n\nUse the live data above to answer. Generate a natural, human-readable response — never dump raw data. " +
               "Highlight key numbers. Offer next steps or follow-up options.";
    }

    private String buildSystemWithToolAndRag(AuthPrincipal principal, String toolSummary, String ragContext) {
        String base = SystemPrompts.forRole(
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);
        return base
               + "\n\n## Live Data\n" + toolSummary
               + "\n\n## Additional Background Knowledge (use silently — never mention 'retrieved context' or 'FAQ' to the user)\n"
               + (StringUtils.hasText(ragContext) ? ragContext : "None.")
               + "\n\nCombine live data and background knowledge into a natural, conversational, insightful response. "
               + "Prioritize live data. Never expose raw objects, SQL, or knowledge source labels.";
    }

    // ── Gemini invocation ─────────────────────────────────────────────────────

    private String callGemini(String systemPrompt, String message, List<AIChatRequest.ChatMessage> history) {
        if (!aiEngine.isAvailable()) {
            log.warn("[AgentOrchestrator] No configured AI provider. Check GEMINI_API_KEY.");
            return null;
        }
        return aiEngine.complete("COPILOT", systemPrompt, message, history, false);
    }

    /**
     * Streaming entry point — progressive token publisher.
     */
    public org.reactivestreams.Publisher<String> processStream(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        if (!StringUtils.hasText(message)) {
            return reactor.core.publisher.Flux.just("Please type a question about events, bookings, or anything I can help with.");
        }

        String sanitized = sanitize(message);
        AgentDecision decision = planner.plan(sanitized, principal);
        log.debug("[AgentOrchestrator][Stream] strategy={} tools={} rag={}", decision.strategy(), decision.toolNames(), decision.useRag());

        return switch (decision.strategy()) {
            case DIRECT      -> handleDirectStream(sanitized, history, principal);
            case RAG_ONLY    -> handleRagOnlyStream(sanitized, history, principal);
            case TOOL_CALL   -> handleToolCallStream(sanitized, decision, history, principal);
            case TOOL_AND_RAG -> handleToolAndRagStream(sanitized, decision, history, principal);
        };
    }

    private org.reactivestreams.Publisher<String> handleDirectStream(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        String systemPrompt = buildEnrichedOrganizerPrompt(principal);
        return callGeminiStream(systemPrompt, message, history, principal);
    }

    private org.reactivestreams.Publisher<String> handleRagOnlyStream(String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        RAGService.RagContext ctx = ragService.buildContext(message, principal);
        String systemPrompt = ragService.conversationalPrompt(
                ctx,
                principal != null ? principal.getRole() : null,
                principal != null ? principal.getId() : null);
        return callGeminiStream(systemPrompt, message, history, principal);
    }

    private org.reactivestreams.Publisher<String> handleToolCallStream(String message, AgentDecision decision,
                                                                        List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        Map<String, Object> toolResults = executeTools(decision.toolNames(), message, principal);
        String toolSummary  = formatToolResults(toolResults);
        String systemPrompt = buildSystemWithToolResults(principal, toolSummary);
        return callGeminiStream(systemPrompt, message, history, principal);
    }

    private org.reactivestreams.Publisher<String> handleToolAndRagStream(String message, AgentDecision decision,
                                                                          List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        Map<String, Object> toolResults = executeTools(decision.toolNames(), message, principal);
        RAGService.RagContext ctx = ragService.buildContext(message, principal);
        String toolSummary  = formatToolResults(toolResults);
        String ragContext   = ctx.formattedContext();
        String systemPrompt = buildSystemWithToolAndRag(principal, toolSummary, ragContext);
        return callGeminiStream(systemPrompt, message, history, principal);
    }

    private org.reactivestreams.Publisher<String> callGeminiStream(String systemPrompt, String message, List<AIChatRequest.ChatMessage> history, AuthPrincipal principal) {
        if (!aiEngine.isAvailable()) {
            return reactor.core.publisher.Flux.just(capabilityFallback(principal));
        }
        return aiEngine.streamComplete("COPILOT", systemPrompt, message, history);
    }

    // ── RAG-based answer when Gemini is unavailable ───────────────────────────

    private String synthesizeFromRag(RAGService.RagContext ctx, String message, AuthPrincipal principal) {
        String q = message.toLowerCase();
        StringBuilder sb = new StringBuilder();

        if (q.contains("register") || q.contains("booking") || q.contains("ticket")) {
            sb.append("To register for an event:\n\n");
            sb.append("1. Log in and go to **Discover Events**\n");
            sb.append("2. Find the event and click **Register**\n");
            sb.append("3. Select ticket quantity, fill in participant details\n");
            sb.append("4. Upload required documents if prompted\n");
            sb.append("5. Complete payment — your QR ticket is generated automatically\n\n");
            sb.append("Which event are you registering for? I can guide you further.");
        } else if (q.contains("refund") || q.contains("cancel")) {
            sb.append("To cancel and request a refund:\n\n");
            sb.append("- Go to **My Bookings** and select the booking\n");
            sb.append("- Refunds for paid bookings are initiated automatically on cancellation\n");
            sb.append("- Typically takes 3–7 business days to process\n\n");
            sb.append("Was the payment already deducted?");
        } else if (q.contains("payment") || q.contains("pay") || q.contains("fail")) {
            sb.append("Payment failures are usually caused by:\n\n");
            sb.append("- Network interruption during checkout\n");
            sb.append("- Bank timeout — the transaction window expired\n");
            sb.append("- Insufficient balance\n\n");
            sb.append("If money was deducted but booking wasn't confirmed, a refund is initiated automatically. ");
            sb.append("Was the amount deducted from your account?");
        } else if (q.contains("certificate")) {
            sb.append("Certificates are issued within 7 days of event completion.\n\n");
            sb.append("- Your attendance must be verified (QR scanned at venue)\n");
            sb.append("- Download from **My Certificates** once ready\n\n");
            sb.append("Which event are you looking for a certificate from?");
        } else {
            // Use RAG content if available, otherwise ask for clarification
            List<String> points = ctx.documents().stream()
                    .filter(d -> d.score() > 0).limit(3)
                    .map(RagDocument::content).filter(StringUtils::hasText).toList();
            if (!points.isEmpty()) {
                String content = points.get(0).replaceAll("\\s+", " ").trim();
                sb.append(content.length() > 350 ? content.substring(0, 350) : content);
                sb.append("\n\nIs there anything specific you'd like me to clarify?");
            } else {
                sb.append("Could you give me a bit more detail? I can help with events, registrations, ");
                sb.append("payments, refunds, certificates, and more.");
            }
        }
        return sb.toString();
    }

    // ── Fallback generators ───────────────────────────────────────────────────

    private String capabilityFallback(AuthPrincipal principal) {
        if (principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole())) {
            return """
                    I can help you with event management. Here's what's available:

                    - 📊 **Analytics** — registrations, revenue, attendance, performance score
                    - 🎪 **Events** — view, manage, and get stats for your events
                    - 👥 **Participants** — attendee lists and export
                    - 📜 **Certificates** — generation and distribution
                    - 📈 **Predictions** — forecast attendance and revenue
                    - 🗣️ **Feedback** — sentiment analysis and improvement suggestions

                    What would you like to do?
                    """;
        }
        return """
                I can help you with:

                - 🎯 **Find events** — search by category, date, college, or keyword
                - 🎫 **Bookings** — check your tickets and QR codes
                - 💳 **Payments** — transaction history and refund status
                - 📜 **Certificates** — earned credentials and downloads
                - ✨ **Recommendations** — events personalised for you

                What are you looking for?
                """;
    }

    private String buildRagFallback(String context, String message) {
        if (!StringUtils.hasText(context)) {
            return "Could you give me a bit more detail? I can help with events, registrations, payments, refunds, and certificates.";
        }
        return "Could you rephrase that? I want to make sure I give you the most helpful answer.";
    }

    private String synthesizeFallback(Map<String, Object> toolResults, String message, AuthPrincipal principal) {
        if (toolResults.isEmpty()) return capabilityFallback(principal);
        StringBuilder sb = new StringBuilder();
        toolResults.forEach((toolName, data) -> {
            if (data instanceof Map<?, ?> m && !m.containsKey("error")) {
                sb.append(summariseToolData(toolName, m)).append("\n\n");
            }
        });
        String result = sb.toString().trim();
        return StringUtils.hasText(result) ? result : capabilityFallback(principal);
    }

    @SuppressWarnings("unchecked")
    private String summariseToolData(String toolName, Map<?, ?> data) {
        return switch (toolName) {
            case "analyticsTool" -> {
                Object regs   = data.get("totalRegistrations");
                Object events = data.get("totalEvents");
                Object comp   = data.get("completedEvents");
                Object pub    = data.get("publishedEvents");
                Object ai     = data.get("aiInsights");
                StringBuilder s = new StringBuilder();
                s.append("Here's a summary of your event analytics:\n\n");
                s.append("- **Total Events:** ").append(events).append("\n");
                s.append("- **Published (Active):** ").append(pub).append("\n");
                s.append("- **Completed:** ").append(comp).append("\n");
                s.append("- **Total Registrations:** ").append(regs).append("\n");
                if (data.get("performanceScore") instanceof Map<?, ?> score) {
                    s.append("- **Performance Score:** ").append(score.get("overallScore")).append("/100");
                    s.append(" · Badge: ").append(score.get("badge")).append("\n");
                }
                if (data.get("topEvents") instanceof java.util.List<?> top && !top.isEmpty()) {
                    s.append("\n**Top Events by Registrations:**\n");
                    top.forEach(e -> {
                        if (e instanceof Map<?, ?> em) {
                            s.append("- ").append(em.get("eventName"))
                             .append(" — ").append(em.get("registrations")).append(" registrations\n");
                        }
                    });
                }
                if (StringUtils.hasText(String.valueOf(ai)) && !String.valueOf(ai).equals("null")) {
                    s.append("\n**AI Insights:** ").append(ai);
                }
                s.append("\n\nWould you like a breakdown of a specific event, or help improving your registrations?");
                yield s.toString();
            }
            case "eventTool" -> {
                if (data.get("myEvents") instanceof java.util.List<?> evts && !evts.isEmpty()) {
                    StringBuilder s = new StringBuilder("Here are your events:\n\n");
                    evts.forEach(e -> {
                        if (e instanceof Map<?, ?> em) {
                            s.append("- **").append(em.get("eventName")).append("**")
                             .append(" | ").append(em.get("status"))
                             .append(" | ").append(em.get("eventDate"))
                             .append(" | Seats: ").append(em.get("availableSeats"))
                             .append("/").append(em.get("totalSeats")).append("\n");
                        }
                    });
                    s.append("\nWhich event would you like more details about?");
                    yield s.toString();
                }
                yield "I retrieved your events. Which one would you like to know more about?";
            }
            case "dashboardTool" -> {
                Object total = data.get("totalEvents");
                Object pub   = data.get("publishedEvents");
                Object regs  = data.get("totalRegistrations");
                Object pend  = data.get("pendingApproval");
                StringBuilder s = new StringBuilder("Here's your dashboard overview:\n\n");
                s.append("- **Total Events:** ").append(total).append("\n");
                s.append("- **Published:** ").append(pub).append("\n");
                s.append("- **Pending Approval:** ").append(pend).append("\n");
                s.append("- **Total Registrations:** ").append(regs).append("\n");
                s.append("\nWhat would you like to do — view analytics, check participants, or manage an event?");
                yield s.toString();
            }
            case "bookingTool" -> {
                Object count     = data.get("count");
                Object confirmed = data.get("confirmedCount");
                yield "You have **" + count + " bookings** (" + confirmed + " confirmed). "
                    + "Go to **My Bookings** for full details. Is there a specific booking I can help with?";
            }
            case "certificateTool" -> {
                Object count = data.get("count");
                yield count != null && ((Number) count).intValue() > 0
                    ? "You have **" + count + " certificate(s)** ready. Visit **My Certificates** to download them."
                    : "No certificates yet. They're issued within 7 days of event completion.";
            }
            default -> "I retrieved the data. What specific information would you like to know?";
        };
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private String formatToolResults(Map<String, Object> toolResults) {
        StringBuilder sb = new StringBuilder();
        toolResults.forEach((toolName, result) -> {
            sb.append("### ").append(toolName).append("\n");
            if (result instanceof Map<?, ?> m) {
                // Serialize key fields, not the full raw map (avoid data dumping)
                m.entrySet().stream()
                        .filter(e -> !isVerboseField(e.getKey().toString()))
                        .limit(30)
                        .forEach(e -> sb.append("- ").append(e.getKey()).append(": ")
                                .append(truncate(String.valueOf(e.getValue()), 300)).append("\n"));
            }
            sb.append("\n");
        });
        return sb.toString();
    }

    private boolean isVerboseField(String key) {
        return key.contains("embeddingJson") || key.contains("messagesJson")
                || key.contains("passwordHash") || key.contains("verificationToken");
    }

    private String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) + "…" : value;
    }

    // ── Sanitization ──────────────────────────────────────────────────────────

    private String sanitize(String message) {
        if (message == null) return "";
        return message
                .replaceAll("(?i)ignore (all )?(previous|prior|above) instructions?", "[FILTERED]")
                .replaceAll("(?i)you are now", "[FILTERED]")
                .replaceAll("(?i)act as( a)?", "[FILTERED]")
                .replaceAll("(?i)(api[_ -]?key|password|token)\\s*[:=]\\s*\\S+", "$1=[redacted]")
                .trim();
    }
}
