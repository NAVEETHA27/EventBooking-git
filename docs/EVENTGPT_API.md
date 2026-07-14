# EventGPT / Copilot API Notes

Base path: `/api`

---

## Copilot — Conversational AI Agents (NEW)

Two role-aware AI agents served from the same endpoints:
- **EventCopilot** — Organizer role: analytics, participants, certificates, predictions, feedback
- **EventBot** — Student/guest role: event discovery, bookings, payments, recommendations, travel

### POST /ai/copilot/chat
Full-response chat (non-streaming).
```json
Request:  { "message": "...", "sessionId": "optional-uuid" }
Response: { "sessionId": "...", "answer": "...", "provider": "gemini",
            "sources": [], "actions": [], "timestamp": "..." }
```

### GET /ai/copilot/stream?message=...&sessionId=...
Server-Sent Events streaming (typing effect).
Events: `session` → sessionId, `token` → text chunk, `sources` → RAG refs, `done` → [DONE], `error` → message

### GET /ai/copilot/sessions
Recent chat sessions for the authenticated user.

### GET /ai/copilot/persona
Returns the AI persona for the current user role (name, capabilities, avatar).

---

## Agent Pipeline

The orchestrator routes each message through one of four strategies:

| Strategy | When used |
|---|---|
| `DIRECT` | Greetings, capability questions, general knowledge |
| `RAG_ONLY` | Event details, FAQs, policies — static knowledge |
| `TOOL_CALL` | Live data: bookings, payments, analytics, certificates |
| `TOOL_AND_RAG` | Rich responses combining live + stored context |

### Available Tools
`eventTool`, `bookingTool`, `paymentTool`, `analyticsTool`, `certificateTool`,
`emailTool`, `recommendationTool`, `travelTool`, `feedbackTool`, `dashboardTool`

---

## Chat (legacy — still active)

### POST /ai/chat
Same as `/ai/copilot/chat` — kept for backward compatibility.

### GET /ai/chat/stream?message=...&sessionId=...
SSE streaming — now uses the agent orchestrator (was rule-based before).

### GET /ai/chat/sessions
Recent sessions.

---

## RAG

### POST /ai/rag/index
Roles: ADMIN, ORGANIZER. Indexes events, FAQs, reviews, certificates into vector store.

### GET /ai/rag/status
Vector provider, document count, embedding mode.

---

## Agent Tools (direct invocation)

### GET /ai/agent/tools
Lists available tool names.

### POST /ai/agent/invoke
```json
{ "prompt": "...", "toolName": "optionalTool", "arguments": {} }
```

---

## Conversation Memory
- Last 20 messages persisted per session in `chat_sessions.messages_json`
- Sessions scoped by userId — guests get ephemeral sessions
- `GET /ai/copilot/sessions` returns recent 20 sessions

---

## Environment Variables
```
GEMINI_API_KEY=...
GEMINI_MODEL=gemini-2.5-flash
AI_PROVIDER=gemini
EVENTGPT_MAX_HISTORY_MESSAGES=20
EVENTGPT_MAX_CONTEXT_DOCUMENTS=12
VECTOR_DB_PROVIDER=mysql
```
