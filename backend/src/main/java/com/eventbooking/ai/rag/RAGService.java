package com.eventbooking.ai.rag;

import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RAGService {
    private final RetrieverService retrieverService;
    private final EventKnowledgeRetrievalService formatter;

    public RagContext buildContext(String query, AuthPrincipal principal) {
        List<RagDocument> documents = retrieverService.retrieve(query, principal);
        String formatted = formatter.formatContext(documents);
        boolean sufficient = documents.stream().anyMatch(doc -> doc.score() > 0);
        return new RagContext(documents, formatted, sufficient);
    }

    /**
     * Builds a system prompt that injects RAG context as HIDDEN BACKGROUND KNOWLEDGE.
     *
     * Key principle: Gemini must synthesize the context into a natural response,
     * NOT list it back to the user as search results.
     *
     * The word "retrieved", "FAQ", "document", "context" must never appear in the response.
     */
    public String conversationalPrompt(RagContext context, String role, Long userId) {
        String base = buildBasePersona(role, userId);

        if (!StringUtils.hasText(context.formattedContext())) {
            return base + """

                    ## Background Knowledge
                    No specific platform data found for this query.
                    Use your general knowledge about college event management to answer helpfully.
                    If you genuinely don't know, say so honestly and offer an alternative suggestion.
                    """;
        }

        return base + """

                ## Background Knowledge (use this silently — never mention "retrieved context", "FAQ", or "document" to the user)
                """ + context.formattedContext() + """

                ## Critical Rules
                - Synthesize the background knowledge into a natural, conversational response
                - NEVER say "Based on the retrieved context..." or "FAQ says..." or "Document 1..."
                - NEVER list raw knowledge entries — rewrite everything as natural language
                - If the background knowledge answers the question, answer confidently
                - If it's partial, combine it with your general knowledge
                - If it's irrelevant, use your general knowledge and don't mention the knowledge base
                """;
    }

    /** Legacy — kept for backward compatibility with non-conversational flows */
    public String guardedPrompt(RagContext context) {
        return conversationalPrompt(context, null, null);
    }

    private String buildBasePersona(String role, Long userId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (role != null && "ORGANIZER".equalsIgnoreCase(role)) {
            return com.eventbooking.ai.agent.SystemPrompts.forRole("ORGANIZER", userId);
        }
        if (role != null && "USER".equalsIgnoreCase(role)) {
            return com.eventbooking.ai.agent.SystemPrompts.forRole("USER", userId);
        }
        return com.eventbooking.ai.agent.SystemPrompts.forRole(null, null);
    }

    public record RagContext(List<RagDocument> documents, String formattedContext, boolean sufficient) {}
}
