package com.eventbooking.service;

import com.eventbooking.ai.agent.AgentOrchestrator;
import com.eventbooking.ai.agent.AgentResponse;
import com.eventbooking.ai.rag.RagDocument;
import com.eventbooking.dto.request.AIChatRequest;
import com.eventbooking.dto.response.AIChatResponse;
import com.eventbooking.entity.ChatSession;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conversational Agent Service.
 *
 * Replaces the rule-based ChatService with a true AI agent that:
 * - Maintains 20-message conversation memory per session
 * - Routes through AgentOrchestrator (DIRECT / RAG / TOOL / TOOL+RAG)
 * - Rate-limits to 20 requests/minute per user
 * - Returns streaming-compatible responses
 * - Falls back gracefully when Gemini is unavailable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationalAgentService {

    private final AgentOrchestrator orchestrator;
    private final ConversationMemoryService memoryService;

    // Rate limiting: 20 requests per minute per user/IP
    private final Map<String, List<Long>> rateLimitMap = new ConcurrentHashMap<>();

    @Transactional
    public AIChatResponse chat(AIChatRequest request, AuthPrincipal principal, String remoteAddr) {
        String message = sanitize(request != null ? request.getMessage() : null);

        if (!StringUtils.hasText(message)) {
            return buildResponse("Please type a question — I'm here to help!", null, "fallback", List.of());
        }
        if (message.length() > 1200) {
            return buildResponse("Please keep your message under 1200 characters.", null, "fallback", List.of());
        }
        if (!checkRateLimit(rateKey(principal, remoteAddr))) {
            return buildResponse("You're sending messages quickly. Please wait a moment and try again.", null, "fallback", List.of());
        }

        // Resolve or create session
        ChatSession session = memoryService.resolveSession(
                request != null ? request.getSessionId() : null,
                principal,
                message);

        // Load last 20 messages from session memory
        List<AIChatRequest.ChatMessage> history = memoryService.history(session);

        // Run through the agent orchestrator
        AgentResponse agentResponse;
        try {
            agentResponse = orchestrator.process(message, history, principal);
        } catch (Exception ex) {
            log.error("[ConversationalAgentService] Orchestrator error: {}", ex.getMessage(), ex);
            // Never expose internal errors — give a helpful conversational response
            agentResponse = AgentResponse.fallback(
                    "Something went wrong on my end. Could you try rephrasing your question? "
                  + "I'm here to help with events, bookings, payments, and more.");
        }

        // Persist turn to session memory
        try {
            memoryService.append(session, message, agentResponse.answer());
        } catch (Exception ex) {
            log.warn("[ConversationalAgentService] Memory persist failed: {}", ex.getMessage());
        }

        AIChatResponse response = buildResponse(
                agentResponse.answer(),
                session.getSessionId(),
                agentResponse.provider(),
                agentResponse.sources());
        response.setSessionId(session.getSessionId());
        return response;
    }

    /**
     * Streaming-optimised chat: returns progressive token chunks reactively.
     */
    @Transactional
    public org.reactivestreams.Publisher<String> chatStream(AIChatRequest request, AuthPrincipal principal, String remoteAddr) {
        String message = sanitize(request != null ? request.getMessage() : null);

        if (!StringUtils.hasText(message)) {
            return reactor.core.publisher.Flux.just("Please type a question — I'm here to help!");
        }
        if (message.length() > 1200) {
            return reactor.core.publisher.Flux.just("Please keep your message under 1200 characters.");
        }
        if (!checkRateLimit(rateKey(principal, remoteAddr))) {
            return reactor.core.publisher.Flux.just("You're sending messages quickly. Please wait a moment and try again.");
        }

        ChatSession session = memoryService.resolveSession(
                request != null ? request.getSessionId() : null,
                principal,
                message);

        List<AIChatRequest.ChatMessage> history = memoryService.history(session);

        try {
            StringBuilder responseBuilder = new StringBuilder();
            return reactor.core.publisher.Flux.from(orchestrator.processStream(message, history, principal))
                    .doOnNext(responseBuilder::append)
                    .doOnComplete(() -> {
                        try {
                            memoryService.append(session, message, responseBuilder.toString());
                        } catch (Exception ex) {
                            log.warn("[ConversationalAgentService] Memory append failed in stream: {}", ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.error("[ConversationalAgentService] Orchestrator stream error: {}", ex.getMessage(), ex);
            return reactor.core.publisher.Flux.just("Something went wrong. Please try rephrasing your question.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AIChatResponse buildResponse(String answer, String sessionId, String provider, List<RagDocument> sources) {
        return AIChatResponse.builder()
                .sessionId(sessionId)
                .answer(answer)
                .provider(provider)
                .timestamp(LocalDateTime.now())
                .actions(defaultActions())
                .sources(sources.stream().limit(6).map(doc -> AIChatResponse.Source.builder()
                        .type(doc.sourceType())
                        .id(doc.sourceId())
                        .title(doc.title())
                        .build()).toList())
                .build();
    }

    private List<AIChatResponse.QuickAction> defaultActions() {
        return List.of(
                AIChatResponse.QuickAction.builder().label("Browse Events").path("/events").build(),
                AIChatResponse.QuickAction.builder().label("My Bookings").path("/bookings").build(),
                AIChatResponse.QuickAction.builder().label("Help & FAQ").path("/help").build()
        );
    }

    private boolean checkRateLimit(String key) {
        long now = System.currentTimeMillis();
        List<Long> times = rateLimitMap.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (times) {
            times.removeIf(t -> now - t > 60_000);
            if (times.size() >= 20) return false;
            times.add(now);
            return true;
        }
    }

    private String rateKey(AuthPrincipal principal, String remoteAddr) {
        if (principal != null) return "user:" + principal.getId();
        return "ip:" + (remoteAddr != null ? remoteAddr : "unknown");
    }

    private String sanitize(String message) {
        if (message == null) return "";
        return message
                .replaceAll("(?i)(api[_ -]?key|password|token)\\s*[:=]\\s*\\S+", "$1=[redacted]")
                .trim();
    }
}
