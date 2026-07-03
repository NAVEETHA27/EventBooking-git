# Requirements Document

## Introduction

This document specifies the complete feature upgrade for an existing Spring Boot 3.2.5 + React 18 college event booking system. The existing system has JWT authentication, role-based access (USER / ORGANIZER), event CRUD, booking/payment models, and a React frontend with Tailwind CSS. The upgrade addresses ten identified gaps: JWT hardening, stricter RBAC with an ADMIN role, landing page access control, landing page redesign, a transaction history module, profile page stability, separated role-based dashboards, database schema improvements, security enhancements, and code quality cleanup.

## Glossary

- **System**: The full-stack college event booking application (Spring Boot backend + React frontend).
- **Auth_Service**: The Spring Boot `AuthService` class that handles registration, login, email verification, and password reset.
- **JWT_Provider**: The `JwtTokenProvider` Spring component that issues, signs, and validates JWT access and refresh tokens.
- **Auth_Filter**: The `JwtAuthenticationFilter` Spring Security filter that intercepts every HTTP request and validates the bearer token.
- **Auth_Context**: The React `AuthContext` / `AuthProvider` that stores the authenticated user state, `login()`, `logout()`, and `updateUser()` helpers.
- **Token_Store**: The browser `localStorage`, which holds keys `eb_token` (access token) and `eb_user` (user JSON).
- **Guest_Route**: The React `GuestRoute` wrapper that redirects authenticated users away from public-only pages.
- **Private_Route**: The React `PrivateRoute` wrapper that redirects unauthenticated users to `/login`.
- **Landing_Page**: The React `Landing.jsx` page served at `/`, presenting the college-focused light-theme UI.
- **Home_Page**: The legacy React `Home.jsx` page with a dark theme, now superseded by Landing_Page.
- **User_Dashboard**: The React page at `/dashboard` accessible only to users with role `USER`.
- **Organizer_Dashboard**: The React page at `/organizer/dashboard` accessible only to users with role `ORGANIZER`.
- **Transaction**: A MySQL entity (table `transactions`) that records each financial operation with a globally unique `txnId` (UUID-based), linked to a `User` and an `Event`.
- **Payment**: The existing MySQL entity (table `payments`) that records payment attempts linked to a `Booking`.
- **Profile_Page**: React pages (`/profile` for USER, `/organizer/profile` for ORGANIZER) that allow viewing and editing account details.
- **Error_Boundary**: A React component that catches JavaScript runtime errors in its subtree and displays a fallback UI.
- **Rate_Limiter**: A Spring component that limits the number of requests from a single IP address within a time window.
- **ROLE_USER**: Spring Security authority granted to registered students.
- **ROLE_ORGANIZER**: Spring Security authority granted to event organizers.
- **ROLE_ADMIN**: Spring Security authority granted to platform administrators.
- **Refresh_Token**: A long-lived JWT used exclusively to obtain a new access token without re-authenticating.
- **Access_Token**: A short-lived JWT (15 minutes) used to authenticate API requests.
- **TXN_ID**: A human-readable unique transaction identifier in the format `TXN-<UUID>`.

---

## Requirements

### Requirement 1: JWT Hardening — Access Token Expiry and Automatic Logout

**User Story:** As a registered user or organizer, I want to be automatically signed out when my session expires, so that my account is protected if I leave the browser unattended.

#### Acceptance Criteria

1. THE JWT_Provider SHALL issue Access_Tokens with a maximum lifetime of 15 minutes.
2. THE JWT_Provider SHALL issue Refresh_Tokens with a maximum lifetime of 7 days.
3. WHEN the Auth_Filter receives a request carrying an expired Access_Token, THE Auth_Filter SHALL return HTTP 401 with error code `TOKEN_EXPIRED`.
4. WHEN the API service in the frontend receives an HTTP 401 response with error code `TOKEN_EXPIRED`, THE Auth_Context SHALL attempt a silent token refresh by calling `POST /auth/refresh-token` with the stored Refresh_Token before retrying the original request.
5. IF the refresh token call fails or the Refresh_Token is absent, THEN THE Auth_Context SHALL call `logout()`, clear the Token_Store, and redirect the browser to `/login`.
6. THE Auth_Service SHALL expose a `POST /auth/refresh-token` endpoint that accepts a valid Refresh_Token and returns a new Access_Token and a new Refresh_Token.
7. WHEN a Refresh_Token has already been used once, THE Auth_Service SHALL invalidate it and reject any further use of the same token with HTTP 401.
8. THE Token_Store SHALL store both the Access_Token under key `eb_token` and the Refresh_Token under key `eb_refresh_token`.

---

### Requirement 2: Role-Based Access Control — ADMIN Role and Organizer Isolation

**User Story:** As a platform administrator, I want a dedicated ADMIN role with protected endpoints, so that I can manage users and organizers without interfering with their separate portals.

#### Acceptance Criteria

1. THE System SHALL recognise exactly three roles: `ROLE_USER`, `ROLE_ORGANIZER`, and `ROLE_ADMIN`.
2. THE SecurityConfig SHALL restrict all routes under `/admin/**` exclusively to principals bearing `ROLE_ADMIN`.
3. THE SecurityConfig SHALL restrict all routes under `/organizer/**` exclusively to principals bearing `ROLE_ORGANIZER`.
4. WHEN a principal bearing `ROLE_USER` requests a resource under `/organizer/**`, THE System SHALL return HTTP 403.
5. WHEN a principal bearing `ROLE_ORGANIZER` requests a resource under `/admin/**`, THE System SHALL return HTTP 403.
6. THE User entity SHALL declare `UserRole` as `USER` or `ADMIN` only; the `ORGANIZER` role SHALL reside exclusively in the `Organizer` entity.
7. WHEN an organizer accesses event management endpoints, THE System SHALL verify that the requested event's `organizer_id` matches the authenticated organizer's ID before allowing write operations.
8. IF an organizer attempts to modify an event owned by a different organizer, THEN THE System SHALL return HTTP 403 with message `"Access denied: event belongs to another organizer"`.

---

### Requirement 3: Landing Page Access Control

**User Story:** As an authenticated user or organizer, I want to be redirected to my dashboard when I navigate to the landing page, so that I am not shown a guest-only marketing page after login.

#### Acceptance Criteria

1. THE App_Router SHALL wrap the `/` route with the Guest_Route component identical to how `/login` and `/register` are currently wrapped.
2. WHEN an authenticated principal with role `USER` navigates to `/`, THE Guest_Route SHALL redirect the browser to `/dashboard`.
3. WHEN an authenticated principal with role `ORGANIZER` navigates to `/`, THE Guest_Route SHALL redirect the browser to `/organizer/dashboard`.
4. WHILE the Auth_Context is loading the stored session, THE Guest_Route SHALL display a full-screen Spinner and SHALL NOT perform any redirect.
5. WHEN an unauthenticated visitor navigates to `/`, THE Guest_Route SHALL render the Landing_Page without redirection.

---

### Requirement 4: Landing Page Redesign

**User Story:** As a prospective student or organizer visiting the platform for the first time, I want to see an informative, visually appealing landing page with college-specific content, so that I understand the platform's value and can register or browse events quickly.

#### Acceptance Criteria

1. THE Landing_Page SHALL use a light theme with a primary palette of blue (`#1565C0`) and red (`#D32F2F`) on a background of `#F0F4FF`.
2. THE Landing_Page SHALL render a Hero section containing a headline, subheadline, a dual-field search form (keyword + college name), and quick-filter tags.
3. WHEN a user submits the search form on the Landing_Page, THE Landing_Page SHALL navigate to `/events` with `keyword` and `collegeName` as URL query parameters.
4. THE Landing_Page SHALL render a Stats section displaying at minimum four platform statistics: total events hosted, total students reached, number of colleges, and satisfaction rate.
5. THE Landing_Page SHALL render a Two-Mode section with separate cards for students (linking to `/register`) and organizers (linking to `/register?role=organizer`).
6. THE Landing_Page SHALL render a Categories section displaying at minimum eight college event categories, each as a clickable chip that navigates to `/events?category=<value>`.
7. THE Landing_Page SHALL render a Featured Events section showing up to eight upcoming public events fetched from `GET /events`.
8. THE Landing_Page SHALL render a CTA section with a button linking to `/register?role=organizer`.
9. THE Home_Page (`Home.jsx`) SHALL be removed from the project. THE App_Router SHALL no longer reference `Home.jsx` in any route definition.
10. WHILE the featured events data is loading, THE Landing_Page SHALL display the Spinner component in place of the event grid.

---

### Requirement 5: Transaction History Module

**User Story:** As a registered user, I want to view a chronological history of all my payment transactions, so that I can track my spending and verify past bookings. As an organizer, I want to see revenue transactions for my events, so that I can reconcile income.

#### Acceptance Criteria

1. THE System SHALL define a `Transaction` JPA entity with fields: `id` (auto-increment), `txnId` (VARCHAR 60, unique, not null), `amount` (DECIMAL 10,2, not null), `paymentStatus` (enum: `PENDING`, `SUCCESS`, `FAILED`), `paymentDate` (DATETIME), `user` (FK → users.id), `event` (FK → events.id).
2. WHEN a new Transaction is persisted, THE System SHALL generate `txnId` in the format `TXN-<UUID>` before saving.
3. THE System SHALL expose `GET /transactions` returning a paginated list of Transaction records belonging to the authenticated `ROLE_USER`, ordered by `paymentDate` descending.
4. THE System SHALL expose `GET /organizer/transactions` returning a paginated list of Transaction records for all events owned by the authenticated `ROLE_ORGANIZER`, ordered by `paymentDate` descending.
5. WHEN a Booking transitions to status `CONFIRMED`, THE System SHALL automatically create a Transaction record with `paymentStatus = SUCCESS` linked to the booking's user and event.
6. WHEN a Booking transitions to status `CANCELLED`, THE System SHALL automatically create a Transaction record with `paymentStatus = FAILED` linked to the booking's user and event.
7. THE Frontend SHALL provide a route `/transactions` accessible only to `ROLE_USER` that renders a paginated table of the user's Transaction records showing columns: TXN ID, Event Name, Amount, Status, Date.
8. THE Organizer_Dashboard SHALL include a Revenue Summary section displaying total revenue and a list of recent transactions fetched from `GET /organizer/transactions`.
9. IF a request to `GET /transactions` is made by an unauthenticated principal, THEN THE System SHALL return HTTP 401.
10. THE Transaction entity SHALL NOT be used as a replacement for the existing Payment entity; both SHALL coexist and serve their distinct purposes.

---

### Requirement 6: Profile Page Stability

**User Story:** As a user or organizer, I want my profile page to load reliably without runtime crashes, so that I can always view and update my account details.

#### Acceptance Criteria

1. WHEN the Profile_Page renders before the profile API response has returned, THE Profile_Page SHALL display the Spinner component and SHALL NOT attempt to access properties of the undefined profile object.
2. IF the profile API call returns an error, THEN THE Profile_Page SHALL render an Error_Boundary fallback displaying the error message and a retry button instead of crashing.
3. THE Profile_Page SHALL access all profile fields using null-safe expressions (optional chaining or explicit null checks) before rendering.
4. WHEN a user submits the profile update form, THE Profile_Page SHALL disable the submit button and display a loading indicator until the API responds.
5. WHEN the profile picture upload succeeds, THE Profile_Page SHALL update the displayed avatar without requiring a full page reload.
6. THE Organizer Profile_Page SHALL follow the same null-safety, loading-state, and Error_Boundary rules specified in criteria 1–5 of this requirement.
7. THE System SHALL provide `GET /organizer/profile` and `PUT /organizer/profile` endpoints that return and accept organizer profile fields including: `organizerName`, `organizationName`, `email`, `phone`, `address`, `website`, `description`, `organizationLogo`.

---

### Requirement 7: Separated Role-Based Dashboards

**User Story:** As a student user, I want a dashboard showing my registered events and notifications. As an organizer, I want a dashboard showing my events, revenue, and attendee registrations.

#### Acceptance Criteria

1. THE User_Dashboard SHALL display four sections: registered events list, upcoming events list, transaction history summary (latest 5 entries), and unread notifications count.
2. THE User_Dashboard SHALL fetch registered events from `GET /bookings` and SHALL display each booking's event name, date, status, and a link to the booking detail page.
3. THE User_Dashboard SHALL fetch unread notification count from `GET /notifications/unread` and SHALL display it as a badge.
4. THE Organizer_Dashboard SHALL display four metric cards: Total Events, Total Revenue (summed from Payment records), Active Events count, and Total Attendees count.
5. THE Organizer_Dashboard SHALL render a Revenue chart showing daily revenue for the last 30 days, fetched from `GET /organizer/dashboard`.
6. THE Organizer_Dashboard SHALL render a recent events table showing event name, category, date, seat availability, and status.
7. WHEN the Organizer_Dashboard data is loading, THE Organizer_Dashboard SHALL display Spinner components in place of each section's content.
8. THE User_Dashboard SHALL NOT display organizer-specific sections such as revenue or event management controls.
9. THE Organizer_Dashboard SHALL NOT display user-specific sections such as personal bookings or personal transaction history.

---

### Requirement 8: Database Schema Improvements

**User Story:** As a developer, I want the database schema to include the Transaction table with correct foreign key constraints, so that transaction data is referentially consistent.

#### Acceptance Criteria

1. THE schema.sql file SHALL include a `CREATE TABLE IF NOT EXISTS transactions` statement with columns: `id`, `txn_id`, `amount`, `payment_status`, `payment_date`, `user_id` (FK → users.id), `event_id` (FK → events.id), `created_at`.
2. THE `transactions.user_id` foreign key SHALL reference `users.id` with `ON DELETE CASCADE`.
3. THE `transactions.event_id` foreign key SHALL reference `events.id` with `ON DELETE CASCADE`.
4. THE `transactions.txn_id` column SHALL have a `UNIQUE` index named `idx_txn_id`.
5. THE events table SHALL include columns `college_name` and `department_name` if not already present, with VARCHAR(200) and VARCHAR(150) types respectively.
6. THE events table SHALL include columns `has_certificate` (BOOLEAN DEFAULT FALSE) and `registration_deadline` (DATE) if not already present.
7. WHEN the application starts, THE JPA auto-DDL setting SHALL be set to `validate` in production and `update` in development to prevent unintended schema destruction.

---

### Requirement 9: Security Enhancements

**User Story:** As a platform operator, I want the API to be hardened against common attacks, so that user data and the system remain secure.

#### Acceptance Criteria

1. THE System SHALL sanitize all user-supplied string inputs in request bodies by stripping leading and trailing whitespace before persistence.
2. THE Auth_Service SHALL enforce a maximum of 5 consecutive failed login attempts for User accounts; on the 5th failure the account SHALL be locked and a notification SHALL be sent to the user's registered email.
3. THE System SHALL apply rate limiting of 20 requests per minute per IP address on all `/auth/**` endpoints.
4. THE System SHALL include a global exception handler (already present) that intercepts all unhandled exceptions and returns a structured JSON error response with fields: `status`, `message`, `timestamp`, `path`.
5. IF a request body exceeds 5 MB, THEN THE System SHALL return HTTP 413 with message `"Request body too large"`.
6. THE SecurityConfig SHALL set the `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` response headers on all API responses.
7. THE Auth_Filter SHALL reject tokens whose `iss` claim does not match the configured application issuer value.
8. WHEN a password reset token has expired (older than 1 hour), THE Auth_Service SHALL return HTTP 400 with message `"Password reset token has expired"` and SHALL NOT update the password.

---

### Requirement 10: Code Quality and Consistency

**User Story:** As a developer, I want the frontend codebase to have a consistent theme, no dead code, and complete organizer pages, so that the UI is maintainable and behaves predictably.

#### Acceptance Criteria

1. THE `Home.jsx` file SHALL be deleted from the project and SHALL NOT be importable or routable from any component.
2. THE App_Router SHALL import and render only `Landing.jsx` for the `/` route; no references to `Home.jsx` SHALL exist in `App.jsx`.
3. THE `organizer/CreateEvent.jsx` page SHALL render a form with fields for event name, description, category, event type, date, time, venue, ticket price, total seats, visibility, and banner upload.
4. THE `organizer/EditEvent.jsx` page SHALL pre-populate all form fields with the existing event data fetched from `GET /events/:id` and SHALL submit changes via `PUT /events/:id`.
5. THE `organizer/Attendees.jsx` page SHALL display a paginated table of all attendees across the organizer's events, showing attendee name, email, event name, booking date, and booking status.
6. THE `organizer/Profile.jsx` page SHALL render the organizer's profile details and follow the same null-safety and Error_Boundary rules specified in Requirement 6.
7. THE entire frontend SHALL use the light theme defined in `index.css`; no component SHALL apply a dark background (`#111`, `#1a1a1a`, `bg-gray-900`, `bg-black`, or equivalent) as a page-level background.
8. THE `index.css` file SHALL define all shared utility classes (`btn-primary`, `input-field`, `badge`, `badge-green`, `badge-red`, `badge-yellow`, `badge-blue`, `badge-gray`, `shadow-card`, `data-table`, `section-title`) that are referenced across components.
