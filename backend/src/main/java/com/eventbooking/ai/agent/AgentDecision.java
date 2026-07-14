package com.eventbooking.ai.agent;

import java.util.List;

/**
 * Represents the AI planner's routing decision for a given user message.
 */
public record AgentDecision(
        Strategy strategy,
        List<String> toolNames,
        boolean useRag,
        String reasoning
) {
    public enum Strategy {
        /** Answer directly with Gemini — general knowledge / greeting / clarification */
        DIRECT,
        /** Search RAG only — event-specific data without live tool execution */
        RAG_ONLY,
        /** Call one or more tools to fetch live data */
        TOOL_CALL,
        /** Execute tools AND enrich with RAG before generating final response */
        TOOL_AND_RAG
    }

    public static AgentDecision direct() {
        return new AgentDecision(Strategy.DIRECT, List.of(), false, "General knowledge question");
    }

    public static AgentDecision ragOnly() {
        return new AgentDecision(Strategy.RAG_ONLY, List.of(), true, "Event-specific knowledge lookup");
    }

    public static AgentDecision toolCall(List<String> tools) {
        return new AgentDecision(Strategy.TOOL_CALL, tools, false, "Live data needed via tools");
    }

    public static AgentDecision toolAndRag(List<String> tools) {
        return new AgentDecision(Strategy.TOOL_AND_RAG, tools, true, "Combine live tool data with RAG context");
    }
}
