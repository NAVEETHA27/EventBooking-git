# EventGPT Implementation Roadmap

## Completed in this increment

- Phase 1: Chat session persistence, memory service, Gemini env mapping, streaming endpoint, and `POST /api/ai/chat` compatibility.
- Phase 2: RAG service boundary, chunking, embedding cache, vector document store, retriever, guarded prompts, and batch index endpoint.
- PGVector infra: optional Docker PGVector service, `backend/sql/pgvector_schema.sql`, and direct JDBC vector provider with MySQL fallback. Application defaults to MySQL fallback; set `VECTOR_DB_PROVIDER=pgvector` for direct PGVector mode.
- Phase 3: Structured agent tool API with guarded tool execution.
- Phase 4: Organizer EventGPT copilot UI with history, suggested prompts, typing state, sources, and agent tool results.

## Next Phases

1. Recommendation AI
   - Expand recommendation scoring with skills, ratings, certificates, attendance, department, college, history, and proximity.
   - Keep Discover page sections: Recommended, Trending, Near You, Popular, AI Picks.

2. Travel AI
   - Store generated travel output per event to avoid repeated Gemini calls.
   - Use event venue, coordinates, Google Maps URL, transport hints, weather, hotel, parking, and restaurant fields.

3. NLP Search
   - Replace lightweight parser with a strict JSON parser plus fallback keyword search.
   - Preserve existing `/api/events` search behavior.

4. Feedback AI
   - Persist sentiment, spam/duplicate flags, positive/negative aspects, improvement suggestions, organizer score, event score, and review summary.

5. Certificate AI
   - Add template upload metadata and generated certificate templates.
   - Keep certificate issue flow after event completion and attendance verification.

6. Prediction AI
   - Add stored prediction snapshots for attendance, capacity, food, revenue, certificates, no-show, engagement, and success score.

7. Portfolio AI
   - Generate student summaries from attended events, certificates, skills, badges, achievements, and future recommendations.

8. Networking AI
   - Improve collaboration suggestions with shared events, skills, departments, ratings, certificates, and interests.

## Testing Strategy

- Backend compile after every phase: `mvn -q -DskipTests compile`.
- Backend startup after every phase: `mvn spring-boot:run`; validate `/api/actuator/health`.
- Vector status: call `GET /api/ai/rag/status` as organizer/admin after startup.
- PGVector activation: add/ensure PostgreSQL JDBC runtime availability, set `VECTOR_DB_PROVIDER=pgvector`, run the Docker `pgvector` service, and call `/api/ai/rag/index`.
- Frontend build after UI changes: `npm.cmd run build`.
- Regression smoke tests:
  - Auth login/register/refresh.
  - Event list/detail/create/update/publish.
  - Booking and payment checkout.
  - Recommendations Discover page.
  - Organizer analytics and attendees.
  - EventGPT chat, sessions, RAG index, and agent invoke.

## Frontend Integration Plan

- Keep `/organizer/copilot` as the only organizer assistant page to avoid duplicate UI.
- Use existing navbar link and fixed top offset to prevent overlap.
- Add EventGPT controls to existing workflows only where they reduce clicks:
  - Create Event: generate description and agenda.
  - Attendees: export, reminders, participant questions.
  - Analytics: prediction and revenue prompts.
  - Event Detail: travel AI display.
  - Discover: AI Picks section.
