# Implementation Plan: College Event Booking System Upgrade

## Overview

This plan upgrades the existing Spring Boot 3.2.5 + React 18 college event booking system across ten areas: JWT hardening with silent token refresh, RBAC with ADMIN role, landing page access control and redesign, a Transaction History module, profile page stability with error boundaries, separated role-based dashboards, database schema improvements, security enhancements (rate limiting, account lockout, input sanitisation), and frontend code quality cleanup.

Implementation proceeds backend-first (data model → security → services → controllers) then frontend (auth → routing → pages → UI polish), closing with integration wiring and a final checkpoint.

---

## Tasks

- [x] 1. Database Schema and JPA Entity Setup
  - [x] 1.1 Update `sql/schema.sql` with transactions table and events column additions
    - Add `CREATE TABLE IF NOT EXISTS transactions` with all columns, foreign keys (`ON DELETE CASCADE`), and indexes (`idx_txn_id` UNIQUE, `idx_txn_user`, `idx_txn_event`, `idx_txn_status`)
    - Add `ALTER TABLE events ADD COLUMN IF NOT EXISTS college_name`, `department_name`, `has_certificate`, `registration_deadline`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 1.2 Create `Transaction.java` JPA entity
    - Implement entity with `@Table(name="transactions")`, all mapped columns, `PaymentStatus` enum (`PENDING`, `SUCCESS`, `FAILED`), `@CreatedDate createdAt`, `@ManyToOne` joins to `User` and `Event` with `FetchType.LAZY`
    - Add `@Index` annotations matching the schema indexes
    - _Requirements: 5.1, 8.1_

  - [x] 1.3 Add new columns to `Event.java` entity
    - Add `collegeName` (VARCHAR 200), `departmentName` (VARCHAR 150), `hasCertificate` (BOOLEAN DEFAULT false), `registrationDeadline` (LocalDate) fields with correct `@Column` mappings
    - _Requirements: 8.5, 8.6_

  - [x] 1.4 Create `TransactionRepository.java`
    - Extend `JpaRepository<Transaction, Long>`
    - Add `Page<Transaction> findByUserIdOrderByPaymentDateDesc(Long userId, Pageable pageable)`
    - Add `Page<Transaction> findByEventOrganizerIdOrderByPaymentDateDesc(Long organizerId, Pageable pageable)`
    - _Requirements: 5.3, 5.4_

  - [x] 1.5 Add `failedLoginAttempts` and `accountLocked` fields to `User.java` entity
    - Add `int failedLoginAttempts` (default 0) and `boolean accountLocked` (default false) with `@Column` mappings
    - _Requirements: 9.2_

- [ ] 2. JWT Hardening — Backend
  - [x] 2.1 Modify `JwtTokenProvider.java` — token lifetimes and `iss` claim
    - Set `expirationMs = 900000` (15 min), `refreshExpirationMs = 604800000` (7 days)
    - Add `iss` claim = `${jwt.issuer}` to both `generateToken` and `generateRefreshToken`
    - Add `jti` UUID claim to `generateRefreshToken`
    - Add `isTokenExpired(String token)` method that returns `true` on `ExpiredJwtException`
    - Add `extractIssuer(String token)` method
    - _Requirements: 1.1, 1.2, 9.7_

  - [ ] 2.2 Write property test for `JwtTokenProvider` token lifetime (Property 1 & 2)
    - **Property 1: Access token lifetime is exactly 15 minutes** (`exp - iat == 900000 ms`)
    - **Property 2: Refresh token lifetime is exactly 7 days** (`exp - iat == 604800000 ms`)
    - **Validates: Requirements 1.1, 1.2**

  - [x] 2.3 Create `RefreshTokenStore.java`
    - Implement in-memory `ConcurrentHashMap<String, Long>` tracking used JTIs with timestamp
    - Add `markUsed(String jti)`, `isUsed(String jti)`, and `@Scheduled(fixedRate=3600000) evictExpired()` methods
    - _Requirements: 1.7_

  - [ ] 2.4 Write property test for `RefreshTokenStore` single-use enforcement (Property 5)
    - **Property 5: For any JTI already marked used, `isUsed()` always returns `true`**
    - **Validates: Requirements 1.7**

  - [ ] 2.5 Modify `JwtAuthenticationFilter.java` — expired token and issuer validation
    - On `ExpiredJwtException` → write JSON response `{ "errorCode": "TOKEN_EXPIRED", "status": 401, "message": "Access token expired", "timestamp": "...", "path": "..." }` and return without continuing the filter chain
    - On issuer mismatch → return HTTP 401 with `errorCode: "INVALID_ISSUER"`
    - _Requirements: 1.3, 9.7_

  - [ ] 2.6 Write property test for `JwtAuthenticationFilter` expired token response (Property 3)
    - **Property 3: Every expired token always produces HTTP 401 with `errorCode == "TOKEN_EXPIRED"`**
    - **Validates: Requirements 1.3**

  - [ ] 2.7 Write property test for `JwtAuthenticationFilter` issuer validation (Property 4)
    - **Property 4: Every token with wrong `iss` claim always produces HTTP 401 with `errorCode == "INVALID_ISSUER"`**
    - **Validates: Requirements 9.7**

  - [ ] 2.8 Add `POST /auth/refresh-token` endpoint to `AuthController.java`
    - Create `RefreshTokenRequest` DTO record with `@NotBlank String refreshToken`
    - Implement endpoint: validate signature, expiry, issuer, and JTI not-used via `RefreshTokenStore`; mark JTI used; generate and return new `accessToken` + `refreshToken`
    - Add `POST /auth/refresh-token` to `SecurityConfig` `permitAll` list
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 1.8_

  - [ ] 2.9 Write property test for refresh token rotation (Property 5 & 6)
    - **Property 5: For any already-used refresh token JTI, the endpoint always returns HTTP 401**
    - **Property 6: For any valid never-used refresh token, the endpoint always returns HTTP 200 with non-null tokens and `response.refreshToken ≠ input.refreshToken`**
    - **Validates: Requirements 1.6, 1.7**

- [ ] 3. Security Enhancements — Backend
  - [ ] 3.1 Create `RateLimitFilter.java`
    - Implement `OncePerRequestFilter` with `@Order(1)`, targeting `/auth/**` paths only
    - Use `ConcurrentHashMap<String, Deque<Long>>` sliding-window per IP; cap at 20 requests per 60 seconds
    - Return HTTP 429 `{ "message": "Too many requests. Please try again later." }` when limit exceeded
    - Add `@Scheduled(fixedRate=60000) evictExpired()` cleanup
    - Register in `SecurityConfig` before `JwtAuthenticationFilter`
    - _Requirements: 9.3_

  - [ ] 3.2 Write property test for `RateLimitFilter` rate limiting (Property 19)
    - **Property 19: For any IP, all requests beyond the 20th within a 60-second window to `/auth/**` always return HTTP 429**
    - **Validates: Requirements 9.3**

  - [ ] 3.3 Modify `AuthService.java` — account lockout, input sanitisation, password reset expiry
    - On login with `accountLocked == true` → throw `AccountLockedException` (HTTP 423) before checking credentials
    - On bad credentials: increment `failedLoginAttempts`; if count reaches 5 → set `accountLocked = true` and call `EmailService.sendAccountLockedEmail()`; reset `failedLoginAttempts` to 0 on successful login
    - Apply `StringUtils.trimWhitespace()` on all incoming string fields before processing
    - In password reset: check `resetTokenExpiry.isBefore(LocalDateTime.now())` → HTTP 400 `"Password reset token has expired"`
    - _Requirements: 9.1, 9.2, 9.8_

  - [ ] 3.4 Write property tests for account lockout logic (Property 17 & 18)
    - **Property 17: For any sequence of exactly 5 consecutive failed login attempts, `u.accountLocked == true` after the 5th**
    - **Property 18: For any `k < 5` failed attempts followed by a successful login, `u.failedLoginAttempts == 0` and `u.accountLocked == false`**
    - **Validates: Requirements 9.2**

  - [ ] 3.5 Update `SecurityConfig.java` — RBAC, security headers, and body size limit
    - Restrict `/admin/**` to `ROLE_ADMIN` only and `/organizer/**` to `ROLE_ORGANIZER` only
    - Add security headers: `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` via `http.headers()` DSL
    - Add `application.properties`: `spring.servlet.multipart.max-request-size=5MB`, `spring.servlet.multipart.max-file-size=5MB`, `server.tomcat.max-http-form-post-size=5MB`
    - Ensure `GlobalExceptionHandler` handles `MaxUploadSizeExceededException` → HTTP 413 with message `"Request body too large"`
    - _Requirements: 2.1, 2.2, 2.3, 9.4, 9.5, 9.6_

  - [ ] 3.6 Write property tests for RBAC enforcement (Property 7 & 8)
    - **Property 7: For any principal where `role ≠ ROLE_ADMIN`, every request to `/admin/**` always returns HTTP 403**
    - **Property 8: For any principal where `role ≠ ROLE_ORGANIZER`, every request to `/organizer/**` always returns HTTP 403**
    - **Validates: Requirements 2.2, 2.3, 2.4, 2.5**

- [ ] 4. Organizer Ownership Enforcement — Backend
  - [ ] 4.1 Add `assertOwnership` check in `EventService.java`
    - In all write operations (`updateEvent`, `deleteEvent`), call `assertOwnership(organizerId, eventId)` that fetches the event and compares `event.organizer.id` with the authenticated principal's ID; throw `AccessDeniedException` with HTTP 403 and message `"Access denied: event belongs to another organizer"` if they differ
    - _Requirements: 2.6, 2.7, 2.8_

  - [ ] 4.2 Write property test for organizer event ownership (Property 9)
    - **Property 9: For any organizer `o` and any event `e` where `e.organizer_id ≠ o.id`, every write operation by `o` on `e` always returns HTTP 403**
    - **Validates: Requirements 2.7, 2.8**

- [ ] 5. Transaction Module — Backend
  - [ ] 5.1 Create `TransactionService.java`
    - Implement `createTransaction(Booking booking, Transaction.PaymentStatus status)`: generate `txnId = "TXN-" + UUID.randomUUID().toString()`, set `amount` from booking, set `user` and `event` from booking, set `paymentDate` to now, persist and return the saved entity
    - Implement `getUserTransactions(Long userId, Pageable pageable)` returning `Page<TransactionResponse>`
    - Implement `getOrganizerTransactions(Long organizerId, Pageable pageable)` returning `Page<TransactionResponse>`
    - Create `TransactionResponse` DTO record: `Long id`, `String txnId`, `String eventName`, `BigDecimal amount`, `String paymentStatus`, `LocalDateTime paymentDate`
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ] 5.2 Write property test for `TransactionService` `txnId` format (Property 13)
    - **Property 13: For any invocation of `createTransaction(...)`, the returned `txnId` always matches `^TXN-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`**
    - **Validates: Requirements 5.2**

  - [ ] 5.3 Modify `BookingService.java` — hook transaction creation on status transitions
    - When `booking.setBookingStatus(CONFIRMED)` → call `transactionService.createTransaction(booking, SUCCESS)`
    - When `booking.setBookingStatus(CANCELLED)` → call `transactionService.createTransaction(booking, FAILED)`
    - Ensure these calls are inside the same `@Transactional` method as the booking save
    - _Requirements: 5.5, 5.6_

  - [ ] 5.4 Write property tests for transaction creation on booking status change (Property 14 & 15)
    - **Property 14: For every booking transitioning to CONFIRMED, exactly one Transaction with `paymentStatus == SUCCESS` is created**
    - **Property 15: For every booking transitioning to CANCELLED, exactly one Transaction with `paymentStatus == FAILED` is created**
    - **Validates: Requirements 5.5, 5.6**

  - [ ] 5.5 Create `TransactionController.java`
    - `GET /transactions?page=0&size=10` secured with `ROLE_USER` → delegates to `transactionService.getUserTransactions(principal.getId(), pageable)` → returns `ApiResponse<Page<TransactionResponse>>`
    - `GET /organizer/transactions?page=0&size=10` secured with `ROLE_ORGANIZER` → delegates to `transactionService.getOrganizerTransactions(principal.getId(), pageable)`
    - _Requirements: 5.3, 5.4, 5.7, 5.9_

  - [ ] 5.6 Write property test for unauthenticated transaction access (Property 16)
    - **Property 16: For every unauthenticated request to `GET /transactions`, the response is always HTTP 401**
    - **Validates: Requirements 5.9**

- [ ] 6. Organizer Profile Endpoints — Backend
  - [ ] 6.1 Add `GET /organizer/profile` and `PUT /organizer/profile` endpoints to `OrganizerController.java` (or equivalent controller)
    - `GET /organizer/profile` returns organizer fields: `organizerName`, `organizationName`, `email`, `phone`, `address`, `website`, `description`, `organizationLogo`
    - `PUT /organizer/profile` accepts and validates the same fields; apply `StringUtils.trimWhitespace()` before saving
    - Secure both endpoints with `ROLE_ORGANIZER`
    - _Requirements: 6.7_

- [ ] 7. Checkpoint — Backend complete
  - Run `./mvnw test` (or equivalent) and ensure all backend unit tests pass; fix any compilation errors; ask the user if questions arise.

- [ ] 8. Frontend — Auth and API Layer
  - [ ] 8.1 Modify `AuthContext.jsx` — refresh token storage and logout cleanup
    - In `login(authResponse)`: store `authResponse.refreshToken` under key `eb_refresh_token` in `localStorage` in addition to existing keys
    - In `logout()`: also remove `eb_refresh_token` from `localStorage`
    - _Requirements: 1.4, 1.5, 1.8_

  - [ ] 8.2 Modify `frontend/src/services/api.js` — silent token refresh interceptor
    - In the Axios response error interceptor, detect `err.response?.status === 401 && err.response?.data?.errorCode === 'TOKEN_EXPIRED'`
    - If `_retry` flag is not set on config: set `_retry = true`, call `POST /auth/refresh-token` with `{ refreshToken: localStorage.getItem('eb_refresh_token') }`, store new tokens under `eb_token` and `eb_refresh_token`, update `api.defaults.headers.common['Authorization']`, and retry the original request
    - On refresh failure: call `AuthContext.logout()` and redirect to `/login`
    - _Requirements: 1.4, 1.5_

- [ ] 9. Frontend — Routing, GuestRoute, and Code Quality
  - [ ] 9.1 Update `App.jsx` — GuestRoute on `/` and remove `Home.jsx` references
    - Wrap the `/` route with `<GuestRoute>` identical to how `/login` and `/register` are wrapped
    - Delete all `import` statements referencing `Home.jsx`; remove any `<Route>` that renders `Home.jsx`
    - _Requirements: 3.1, 10.1, 10.2_

  - [ ] 9.2 Modify `GuestRoute` component — role-based redirect logic
    - When `loading == true` → render `<Spinner full />` without redirecting
    - When user role is `"USER"` → redirect to `/dashboard`
    - When user role is `"ORGANIZER"` → redirect to `/organizer/dashboard`
    - When user role is `"ADMIN"` → redirect to `/admin/dashboard` (future-proofing)
    - When unauthenticated → render children without redirect
    - _Requirements: 3.2, 3.3, 3.4, 3.5_

  - [ ] 9.3 Write property tests for `GuestRoute` redirect logic (Property 10, 11, 12)
    - **Property 10: For any authenticated user with `role == "USER"` at `/`, `GuestRoute` always redirects to `/dashboard`**
    - **Property 11: For any authenticated user with `role == "ORGANIZER"` at `/`, `GuestRoute` always redirects to `/organizer/dashboard`**
    - **Property 12: For any unauthenticated visitor at `/`, `GuestRoute` always renders `Landing.jsx` without redirecting**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.5**

  - [ ] 9.4 Delete `Home.jsx` from the project
    - Delete `frontend/src/pages/Home.jsx`
    - Verify no imports or route references remain anywhere in the frontend
    - _Requirements: 4.9, 10.1, 10.2_

- [ ] 10. Frontend — Shared CSS Utility Classes
  - [ ] 10.1 Update `frontend/src/index.css` — define all shared utility classes
    - Define classes using the light theme (`#1565C0` blue, `#D32F2F` red, `#F0F4FF` background): `btn-primary`, `btn-ghost`, `btn-outline`, `input-field`, `badge`, `badge-green`, `badge-red`, `badge-yellow`, `badge-blue`, `badge-gray`, `shadow-card`, `data-table`, `section-title`
    - Ensure no component uses dark backgrounds (`#111`, `#1a1a1a`, `bg-gray-900`, `bg-black`, or equivalent) as page-level backgrounds
    - _Requirements: 10.7, 10.8_

- [ ] 11. Frontend — ErrorBoundary Component
  - [ ] 11.1 Create `frontend/src/components/common/ErrorBoundary.jsx`
    - Implement class component with `state = { hasError: false, error: null }`
    - Implement `static getDerivedStateFromError(error)` and `componentDidCatch(error, info)`
    - Render fallback UI with error message and a "Retry" button that calls `this.props.onRetry?.()` and resets state
    - _Requirements: 6.2_

- [ ] 12. Frontend — Landing Page Redesign
  - [ ] 12.1 Rewrite `frontend/src/pages/Landing.jsx` — Hero and Stats sections
    - Apply light theme: background `#F0F4FF`, primary `#1565C0`, accent `#D32F2F`
    - Render `HeroSection`: headline, subheadline, dual-field search form (keyword + collegeName inputs), quick-filter tags (Workshop, Seminar, Cultural, Sports, Technical…)
    - On search form submit: navigate to `/events?keyword=<value>&collegeName=<value>`
    - Render `StatsSection`: four stat cards (Events Hosted, Students Reached, Colleges, Satisfaction Rate)
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ] 12.2 Add remaining sections to `Landing.jsx` — TwoMode, Categories, Featured Events, CTA
    - Render `TwoModeSection`: student card → `/register`, organizer card → `/register?role=organizer`
    - Render `CategoriesSection`: 8+ clickable chips, each navigates to `/events?category=<value>`
    - Render `FeaturedEventsSection`: fetch `GET /events?featured=true&size=8`; show `<Spinner />` while loading; render up to 8 event cards using existing `EventCard` component
    - Render `CTASection`: "Become an Organizer" button → `/register?role=organizer`
    - _Requirements: 4.5, 4.6, 4.7, 4.8, 4.10_

- [ ] 13. Frontend — Profile Pages Stability
  - [ ] 13.1 Update `frontend/src/pages/user/Profile.jsx` — null-safety, loading state, ErrorBoundary
    - Display `<Spinner />` when profile API response has not yet returned
    - Access all profile fields with optional chaining (e.g. `profile?.name`) — no direct property access on potentially undefined objects
    - Wrap profile content in `<ErrorBoundary onRetry={refetch}>` imported from `common/ErrorBoundary.jsx`
    - Disable submit button and show loading indicator while PUT request is in flight
    - On profile picture upload success: update displayed avatar without full page reload
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ] 13.2 Update `frontend/src/pages/organizer/Profile.jsx` — same null-safety and stability rules
    - Apply identical null-safety, loading-state, and `ErrorBoundary` rules as Requirement 6.6
    - Use `GET /organizer/profile` and `PUT /organizer/profile` endpoints
    - Render fields: `organizerName`, `organizationName`, `email`, `phone`, `address`, `website`, `description`, `organizationLogo`
    - _Requirements: 6.6, 6.7, 10.6_

  - [ ] 13.3 Write property test for Profile null-safety (Property 20)
    - **Property 20: For any `profile` value including `null`, `undefined`, and partial objects, `ProfileContent` render never throws a `TypeError`**
    - **Validates: Requirements 6.1, 6.3**

- [ ] 14. Frontend — Organizer Pages Completion
  - [ ] 14.1 Complete `frontend/src/pages/organizer/CreateEvent.jsx`
    - Render form with fields: event name, description, category, event type, date, time, venue, ticket price, total seats, visibility, and banner upload
    - Use `react-hook-form` + `yup` validation; use `input-field` and `btn-primary` utility classes
    - Submit via `POST /events`
    - _Requirements: 10.3_

  - [ ] 14.2 Complete `frontend/src/pages/organizer/EditEvent.jsx`
    - On mount, fetch existing event from `GET /events/:id` and pre-populate all form fields
    - Submit changes via `PUT /events/:id`
    - Use same form fields and validation as `CreateEvent.jsx`
    - _Requirements: 10.4_

  - [ ] 14.3 Complete `frontend/src/pages/organizer/Attendees.jsx`
    - Render paginated table showing: attendee name, email, event name, booking date, booking status
    - Use `data-table` and `badge` utility classes; fetch data from the appropriate bookings endpoint with organizer scope
    - _Requirements: 10.5_

- [ ] 15. Frontend — Transaction History Page
  - [ ] 15.1 Create `frontend/src/pages/user/Transactions.jsx`
    - Protect route with `<PrivateRoute roles={['USER']}>`; add route `/transactions` in `App.jsx`
    - Use `react-query` with cache key `['transactions', page]`; fetch `GET /transactions?page=<page>&size=10`
    - Render paginated table: TXN ID, Event Name, Amount, Status (badge-colored), Date
    - _Requirements: 5.7_

- [ ] 16. Frontend — User Dashboard Update
  - [ ] 16.1 Update `frontend/src/pages/user/Dashboard.jsx` — registered events, notifications, and transaction summary
    - Add "My Registered Events" section: fetch `GET /bookings`; display event name, date, status, and link to booking detail for each entry
    - Add unread notification badge: fetch `GET /notifications/unread`; display count as badge
    - Add "Recent Transactions" section: fetch `GET /transactions?page=0&size=5`; display latest 5 entries
    - Display `<Spinner />` in place of each section while loading
    - Remove any organizer-specific sections (revenue, event management)
    - _Requirements: 7.1, 7.2, 7.3, 7.8_

- [ ] 17. Frontend — Organizer Dashboard Update
  - [ ] 17.1 Update `frontend/src/pages/organizer/Dashboard.jsx` — metric cards, revenue summary, and events table
    - Ensure four metric cards are present: Total Events, Total Revenue, Active Events, Total Attendees
    - Add "Revenue Summary" section: fetch `GET /organizer/transactions?page=0&size=5`; display total revenue and recent transaction list
    - Verify revenue chart (daily revenue last 30 days) fetches from `GET /organizer/dashboard`
    - Verify recent events table shows: event name, category, date, seat availability, status
    - Display `<Spinner />` in each section while loading
    - Remove any user-specific sections (personal bookings, personal transaction history)
    - _Requirements: 7.4, 7.5, 7.6, 7.7, 7.9, 5.8_

- [ ] 18. Final Checkpoint — Ensure all tests pass
  - Run all backend tests (`./mvnw test`) and all frontend tests (`npm test --run` or `vitest --run` inside `frontend/`)
  - Ensure no TypeScript/lint errors in frontend; ensure all routes resolve correctly
  - Verify `Home.jsx` no longer exists and `App.jsx` has no reference to it
  - Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for full traceability
- Checkpoints validate progress incrementally — do not skip them
- Property tests validate universally quantified correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The design document specifies that **no new Maven or npm dependencies are required** — all libraries are already present in `pom.xml` and `frontend/package.json`
- Backend language: **Java (Spring Boot 3.2.5)**; Frontend language: **JavaScript/JSX (React 18 + Vite)**
- JPA DDL: set `spring.jpa.hibernate.ddl-auto=update` for development, `validate` for production

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.5"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4"] },
    { "id": 2, "tasks": ["1.5", "2.1", "2.3"] },
    { "id": 3, "tasks": ["2.2", "2.4", "2.5", "2.8", "3.3", "5.1", "8.1"] },
    { "id": 4, "tasks": ["2.6", "2.7", "2.9", "3.1", "3.4", "4.1", "5.3", "5.5", "6.1"] },
    { "id": 5, "tasks": ["3.2", "3.5", "4.2", "5.2", "5.4", "5.6", "8.2"] },
    { "id": 6, "tasks": ["3.6", "9.1", "9.2", "10.1", "11.1"] },
    { "id": 7, "tasks": ["9.3", "9.4", "12.1", "13.1", "13.2", "14.1", "14.2", "14.3"] },
    { "id": 8, "tasks": ["12.2", "13.3", "15.1", "16.1", "17.1"] }
  ]
}
```
