package com.eventbooking.ai.agent;

import com.eventbooking.ai.rag.RagDocument;

import java.util.List;

/**
 * Structured response from the Agent Orchestrator.
 */
public record AgentResponse(
        String answer,
        List<RagDocument> sources,
        String provider,
        String strategy,
        boolean success
) {
    public static AgentResponse success(String answer, List<RagDocument> sources, String provider, String strategy) {
        return new AgentResponse(answer, sources != null ? sources : List.of(), provider, strategy, true);
    }

    public static AgentResponse fallback(String message) {
        return new AgentResponse(message, List.of(), "fallback", "FALLBACK", false);
    }
}
