package com.eventbooking.ai.agent;

import com.eventbooking.security.AuthPrincipal;

import java.util.Map;

/**
 * Contract for all AI agent tools.
 * Each tool executes a focused operation and returns a structured result as a plain Map.
 */
public interface AgentTool {

    /** Unique tool name used by the planner (e.g. "eventTool", "bookingTool") */
    String name();

    /** One-line description used by the planner prompt */
    String description();

    /**
     * Execute the tool.
     *
     * @param input     extracted entities from the user message (e.g. eventId, query, dates)
     * @param principal authenticated user/organizer — may be null for guests
     * @return structured result; never throws; returns error details in result map on failure
     */
    Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal);
}
