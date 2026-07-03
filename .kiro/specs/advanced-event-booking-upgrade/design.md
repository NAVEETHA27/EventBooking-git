# Design Document: Advanced Event Booking Upgrade

## Overview

This design describes the implementation of 13 advanced features on top of the existing Spring Boot 3.2.5 + React 18 college event booking system. The existing codebase already has most entity models, service stubs, and controller skeletons in place (visible in the `model/`, `service/`, and `controller/` packages). This design focuses on completing, wiring, and hardening those partial implementations, adding the missing UI pages, and connecting all pieces into a cohesive system.

The tech stack is unchanged: Spring Boot 3.2.5 (Java 17), MySQL via JPA/Hibernate, MongoDB for notifications, React 18 + Vite + Tailwind CSS + Framer Motion, JWT for auth, Gmail SMTP for email, and SSE for real-time push.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        React 18 SPA                         │
│  Pages: Help, PaymentHistory, RefundTracking, QueueStatus,  │
│  AdminDashboard, Notifications, Profile (User/Org), Bookings│
│  Services: api.js (Axios), EventSource (SSE)                │
└─────────────────────────────┬───────────────────────────────┘
                              │ HTTPS / SSE
┌─────────────────────────────▼───────────────────────────────┐
│              Spring Boot 3.2.5 REST API (:8080/api)         │
│  Controllers: Auth, Event, Booking, Payment, Refund,        │
│               HelpCenter, Admin, Notification, Attendance,  │
│               UserProfile, OrganizerProfile                 │
│  Services: All existing + BookingQueue, Approval, Audit     │
│  Security: JWT filter, SecurityConfig, RateLimitFilter      │
└──────────┬──────────────────────────────────────┬───────────┘
           │ JPA (HikariCP)                        │ Spring Data MongoDB
┌──────────▼──────────┐                 ┌──────────▼──────────┐
│       MySQL 8       │                 │      MongoDB         │
│  Tables: users,     │                 │  Collections:        │
│  organizers, events,│                 │  notifications       │
│  bookings, payments,│                 └─────────────────────┘
│  refunds, audit_logs│
│  booking_queue,     │
│  approval_requests, │
│  faq, tutorial_vids,│
│  attendance,        │
│  profile_locations  │
└─────────────────────┘
```

---

## Components and Interfaces

### Backend Components

| Component | Class | Responsibility |
|---|---|---|
| `HelpCenterService` | existing | FAQ CRUD, video CRUD, search/filter |
| `HelpCenterController` | existing | `GET /help/faqs`, `GET /help/videos`, admin endpoints |
| `PaymentService` | existing (expand) | Payment lifecycle, history pagination |
| `PaymentController` | existing (expand) | `GET /payments/history`, `GET /admin/payments` |
| `RefundService` | existing (expand) | Refund workflow, auto-refund on cancellation |
| `BookingQueueService` | new | Enqueue, process, expire, seat release |
| `ApprovalService` | new | Approval workflow, event status transitions |
| `AdminDashboardService` | existing (expand) | Aggregate stats, recent activity |
| `AuditService` | existing (expand) | Async audit log writes for all key events |
| `NotificationService` | existing (complete) | SSE emitters, MongoDB persistence, event broadcasts |
| `EmailService` | existing (expand) | All transactional email templates |
| `QRCodeService` | new | Generate QR PNG from ticketId, store on disk |
| `AttendanceService` | new | Check-in logic, duplicate scan detection |
| `ProfileLocationService` | existing | Lat/lng persistence in `profile_locations` |

### Frontend Pages

| Route | Component | Role |
|---|---|---|
| `/help` | `HelpCenter.jsx` | Public |
| `/payments/history` | `PaymentHistory.jsx` | USER |
| `/refunds/tracking` | `RefundTracking.jsx` | USER |
| `/queue-status` | `QueueStatus.jsx` | USER |
| `/notifications` | `Notifications.jsx` | USER/ORGANIZER |
| `/profile` | `user/Profile.jsx` | USER |
| `/organizer/profile` | `organizer/Profile.jsx` | ORGANIZER |
| `/admin/dashboard` | `admin/Dashboard.jsx` | ADMIN |
| `/admin/approvals` | `admin/Approvals.jsx` | ADMIN |
| `/bookings/:id` | `user/BookingDetail.jsx` | USER (QR display) |

---

## Data Models

### Database Schema Additions

```sql
-- users table additions (already partially in model)
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS user_code  VARCHAR(20)  UNIQUE,
  ADD COLUMN IF NOT EXISTS city       VARCHAR(100),
  ADD COLUMN IF NOT EXISTS state      VARCHAR(100),
  ADD COLUMN IF NOT EXISTS country    VARCHAR(100),
  ADD COLUMN IF NOT EXISTS pin_code   VARCHAR(20),
  ADD COLUMN IF NOT EXISTS latitude   DECIMAL(10,7),
  ADD COLUMN IF NOT EXISTS longitude  DECIMAL(10,7);

-- organizers table additions
ALTER TABLE organizers
  ADD COLUMN IF NOT EXISTS organizer_code VARCHAR(30) UNIQUE,
  ADD COLUMN IF NOT EXISTS city           VARCHAR(100),
  ADD COLUMN IF NOT EXISTS state          VARCHAR(100),
  ADD COLUMN IF NOT EXISTS country        VARCHAR(100),
  ADD COLUMN IF NOT EXISTS pin_code       VARCHAR(20);

-- events table addition
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30) DEFAULT 'NOT_REQUIRED';

-- New: faq table
CREATE TABLE IF NOT EXISTS faq (
  id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
  category   VARCHAR(80)  NOT NULL,
  question   VARCHAR(500) NOT NULL,
  answer     TEXT         NOT NULL,
  active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at DATETIME,
  updated_at DATETIME,
  INDEX idx_faq_category (category),
  INDEX idx_faq_active   (active)
);

-- New: tutorial_videos table
CREATE TABLE IF NOT EXISTS tutorial_videos (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
  title       VARCHAR(200) NOT NULL,
  description TEXT,
  video_url   VARCHAR(500) NOT NULL,
  thumbnail   VARCHAR(300),
  category    VARCHAR(80),
  active      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  DATETIME,
  updated_at  DATETIME
);

-- New: profile_locations table
CREATE TABLE IF NOT EXISTS profile_locations (
  id           BIGINT         PRIMARY KEY AUTO_INCREMENT,
  owner_id     BIGINT         NOT NULL,
  owner_type   VARCHAR(20)    NOT NULL,
  latitude     DECIMAL(10,7)  NOT NULL,
  longitude    DECIMAL(10,7)  NOT NULL,
  address_line VARCHAR(400),
  city         VARCHAR(100),
  state        VARCHAR(100),
  country      VARCHAR(100),
  pin_code     VARCHAR(20),
  saved_at     DATETIME,
  INDEX idx_profile_loc_owner (owner_id, owner_type)
);

-- booking_queue, approval_requests, audit_logs — already in schema from existing models
-- attendance — already in Attendance.java model
```

### Key JPA Entity Summary

**User.java** (extend existing): add `userCode`, `city`, `state`, `country`, `pinCode`, `latitude`, `longitude` columns. All exist in the model already.

**Organizer.java** (extend existing): add `organizerCode`, `city`, `state`, `country`, `pinCode`. All exist.

**Faq.java** (existing): `id`, `category`, `question`, `answer`, `active`, `createdAt`, `updatedAt`.

**TutorialVideo.java** (existing): `id`, `title`, `description`, `videoUrl`, `thumbnail`, `category`, `active`, `createdAt`.

**BookingQueueEntry.java** (existing): `requestId`, `userId`, `eventId`, `quantity`, `requestTimestamp`, `bookingStatus` (enum), `bookingId`, `message`.

**ApprovalRequest.java** (existing): `id`, `event`, `organizer`, `status` (enum), `reviewNote`, `reviewedBy`, `requestedAt`, `reviewedAt`.

**AuditLog.java** (existing): `id`, `actorId`, `actorType`, `action`, `targetType`, `targetId`, `details`, `createdAt`.

**Attendance.java** (existing): `id`, `booking` (FK), `checkedInAt`, `checkedInBy` (organizer id).

**Payment.java** (existing): statuses `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`, `PARTIALLY_REFUNDED`.

**Refund.java** (existing): statuses `INITIATED`, `REFUND_REQUESTED`, `UNDER_VERIFICATION`, `APPROVED`, `PROCESSING`, `COMPLETED`, `REJECTED`, `FAILED`.

---

## Feature Design Details

### Feature 1: Help Center
- `HelpCenterController` maps `GET /help/faqs` (public) and `GET /help/videos` (public).
- Admin endpoints at `POST|PUT /admin/help/faqs` and `POST|PUT|DELETE /admin/help/videos` require `ROLE_ADMIN`.
- Video file upload: multipart file stored via existing `app.upload.dir` config. File path saved in `video_url`.
- Frontend `HelpCenter.jsx` already exists — complete accordion FAQ and video gallery rendering.

### Feature 2 & 3: Payment and Refund Lifecycle
- `PaymentService.history()` already partially implemented. Extend to return full DTO including `eventName`.
- `RefundService.autoInitiateForEvent(Long eventId)`: fetch all confirmed bookings for event, create one Refund per booking with status `INITIATED`, notify user via SSE and email.
- `PUT /admin/refunds/{id}/approve`: transition to `APPROVED` → `PROCESSING` → eventual `COMPLETED`. Send SSE + email.
- `PUT /admin/refunds/{id}/reject`: transition to `REJECTED`. Send SSE + email with reason.

### Feature 4: Unique IDs
- `User_Code` generation: after `userRepository.saveAndFlush(user)`, set `user.setUserCode("U" + String.format("%06d", user.getId()))`, then `userRepository.save(user)`. This pattern already exists in `AuthService`.
- `Organizer_Code` generation: already implemented as `buildOrganizerCode()` in `AuthService` producing `O{3-char-prefix}{06d-id}`.
- Legacy migration: `@PostConstruct` bean in a `DataMigrationService` that finds users/organizers with null codes and assigns them.

### Feature 5: Improved Registration / Location
- Registration DTOs already accept `city`, `state`, `country`, `pinCode` in `AuthService`.
- Frontend `Register.jsx`: add city/state/country/pincode fields + "Detect My Location" button using `navigator.geolocation`.
- Reverse geocoding: use browser-side OpenStreetMap Nominatim API (free, no API key) to convert lat/lng to address components.
- Backend `POST /profile/location`: persist to `profile_locations` table via `ProfileLocationService`.

### Feature 6: Booking Queue
- `BookingQueueService.enqueue()`: save `BookingQueueEntry` with status `RECEIVED` and `requestTimestamp = now()`.
- `BookingQueueService.processNext()`: `@Scheduled(fixedDelay=500)` method. Fetch oldest `RECEIVED` entry per event using `SELECT … WHERE status='RECEIVED' ORDER BY request_timestamp LIMIT 1 FOR UPDATE`. Use `@Transactional` with pessimistic lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the event row to decrement `available_seats`.
- Timeout expiry: `@Scheduled(fixedDelay=60000)` method scans `PAYMENT_PENDING` entries older than 10 minutes, transitions to `EXPIRED`, restores seats, sends SSE.

### Feature 7: Admin Approval
- `EventService.createEvent()`: after saving event, call `ApprovalService.submitForApproval(event)` which creates `ApprovalRequest` with `PENDING` and sets `event.status = PENDING_APPROVAL`.
- `ApprovalService.approve(id, adminId)`: set status `APPROVED`, set `event.status = PUBLISHED`, send SSE + email to organizer.
- `ApprovalService.reject(id, adminId, note)`: set `REJECTED`, set `event.status = REJECTED`, send SSE + email.

### Feature 8: Admin Dashboard
- `AdminDashboardService.getStats()`: executes count queries for users, organizers, events (by status), payments (by status), refunds (by status), pending approvals.
- `GET /admin/dashboard` returns `AdminDashboardStatsResponse` DTO.
- Paginated sub-resources at `/admin/users`, `/admin/organizers`, `/admin/events`, `/admin/payments`, `/admin/refunds`, `/admin/audit-logs` each return `Page<T>`.

### Feature 9: Enhanced Profile
- `UserProfileController`: `GET /users/me` returns `UserProfileResponse` including `userCode`, all location fields.
- `PUT /users/profile` updates editable fields.
- `POST /users/change-password`: verify current password, update hash.
- `PUT /users/profile-picture`: multipart upload, update `profilePicture` field.
- Frontend `user/Profile.jsx`: render User_Code, photo upload, address section, location section with "Detect My Location".

### Feature 10: SSE Notifications
- `NotificationService` already implements `stream()`, `sendNotification()`, and `emit()`. Complete the wiring so every service calls it at the right moments.
- `GET /notifications/stream`: registered in `SecurityConfig` as authenticated endpoint.
- Frontend: `useNotifications` custom hook opens `EventSource`, appends to notification list, updates unread count.

### Feature 11: Email Notifications
- `EmailService` already exists with SMTP config. Add missing template methods:
  - `sendBookingConfirmationEmail(user, booking, qrCodePath)`
  - `sendRefundStatusEmail(user, refund)`
  - `sendEventApprovalEmail(organizer, event, status, note)`
  - `sendEventCancellationEmail(user, event)`
- All methods annotated `@Async` to not block the calling thread.
- Exception handling: wrap SMTP calls in try-catch, log at WARN on failure.

### Feature 12: QR Codes and Attendance
- `QRCodeService.generateQR(ticketId)`: uses ZXing library (`com.google.zxing:core`) to encode `ticketId` into a 300×300 PNG. Saves file to `{upload.dir}/qr/{ticketId}.png`. Returns the file path.
- `BookingService.confirmBooking()`: after saving booking with `CONFIRMED` status, call `qrCodeService.generateQR(booking.getTicketId())` and update `booking.setQrCodePath(path)`.
- `AttendanceController.checkIn(ticketId)`: look up booking by ticketId; if attendance already exists → 409; if booking cancelled → 400; else create `Attendance` record → 200.
- `GET /bookings/{id}/qr-code`: serve the PNG file from disk as `image/png` response.

### Feature 13: Audit Log
- `AuditService.record()` is already `@Async` and writes to `audit_logs` table via JPA.
- Audit calls are already placed in `AuthService`. Need to add them to: `BookingService`, `PaymentService`, `RefundService`, `EventService`, `AttendanceService`, `ApprovalService`.
- Admin `GET /admin/audit-logs` with `Pageable` and optional filters for `actorType`, `action`, date range.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: FAQ search always returns only matching records

*For any* list of FAQ records and any search term, every record returned by the search function must contain the search term in either the question or the answer field (case-insensitive).

**Validates: Requirements 1.2**

### Property 2: FAQ category filter returns only matching category records

*For any* list of FAQ records and any category value, every record returned by the category filter must have a `category` field equal to the filter value.

**Validates: Requirements 1.3**

### Property 3: Payment history is sorted descending by paid_at

*For any* set of payment records belonging to a user, the list returned by `PaymentService.history()` must be sorted such that for every adjacent pair `(a, b)`, `a.paidAt >= b.paidAt`.

**Validates: Requirements 2.2**

### Property 4: Booking confirmation always creates a SUCCESS payment

*For any* booking that transitions to `CONFIRMED` status, exactly one `Payment` record with `paymentStatus = SUCCESS` linked to that booking must be created.

**Validates: Requirements 2.5**

### Property 5: Event cancellation creates exactly one INITIATED refund per confirmed booking

*For any* event with N confirmed bookings, cancelling the event must result in exactly N new `Refund` records each with status `INITIATED` linked to the respective bookings.

**Validates: Requirements 3.2**

### Property 6: User_Code format is always U + 6-digit zero-padded ID

*For any* user with database `id` equal to N, the generated `userCode` must equal `"U" + String.format("%06d", N)` — a fixed-length 7-character string starting with U.

**Validates: Requirements 4.1**

### Property 7: Organizer_Code always matches pattern O[A-Z]{3}[0-9]{6}

*For any* organizer regardless of organization name content or ID value, the generated `organizerCode` must match the regular expression `^O[A-Z]{3}[0-9]{6}$`.

**Validates: Requirements 4.2**

### Property 8: Queue entries older than 10 minutes in PAYMENT_PENDING always expire

*For any* `BookingQueueEntry` with `bookingStatus = PAYMENT_PENDING` and `requestTimestamp` more than 10 minutes before the current time, the expiry scheduler must set its status to `EXPIRED` and restore the reserved seat count.

**Validates: Requirements 6.6**

### Property 9: New event submission always creates a PENDING approval request

*For any* new event created by an organizer, exactly one `ApprovalRequest` with `status = PENDING` linked to that event must exist, and `event.status` must equal `PENDING_APPROVAL`.

**Validates: Requirements 7.1**

### Property 10: Confirmed booking always generates a non-null QR code path

*For any* booking that transitions to `CONFIRMED` status, the `qrCodePath` field on the booking must be a non-null, non-empty string pointing to an existing file.

**Validates: Requirements 12.1**

### Property 11: Duplicate check-in always returns HTTP 409

*For any* `ticketId` that has already been successfully checked in (an `Attendance` record exists), a second check-in attempt must always return HTTP 409 with `status = "DUPLICATE_SCAN"`.

**Validates: Requirements 12.5**

### Property 12: Audit write failure never propagates to the primary operation

*For any* business operation that triggers an audit log write, if the audit log write throws an exception, the primary operation must still complete and return its normal success response.

**Validates: Requirements 13.6**

---

## Error Handling

| Scenario | HTTP Status | Error Code | Message |
|---|---|---|---|
| Duplicate check-in | 409 | `DUPLICATE_SCAN` | "This ticket has already been used." |
| Cancelled ticket check-in | 400 | `INVALID_TICKET` | "This booking has been cancelled." |
| Tickets sold out | 409 | `TICKETS_SOLD_OUT` | "No seats available for this event." |
| Queue entry expired | 410 | `QUEUE_EXPIRED` | "Your booking window has expired. Please rebook." |
| Incorrect current password | 400 | `WRONG_PASSWORD` | "Current password is incorrect." |
| Refund not found | 404 | `NOT_FOUND` | "Refund record not found." |
| Event not in approvable state | 400 | `INVALID_STATE` | "Event is not in a state that can be reviewed." |
| Email delivery failure | — | (log only) | Logged at WARN; no exception propagated |
| Audit log write failure | — | (log only) | Logged at WARN; no exception propagated |

All unhandled exceptions are caught by the existing `GlobalExceptionHandler` and returned as structured JSON with `status`, `message`, `timestamp`, `path`.

---

## Testing Strategy

### Unit Tests (example-based)
- `HelpCenterServiceTest`: verify FAQ creation, update, search with mock repository.
- `RefundServiceTest`: verify auto-refund creation on event cancellation using a mock booking list.
- `ApprovalServiceTest`: verify state transitions for approve/reject/modification-requested.
- `QRCodeServiceTest`: verify QR file is written and path is returned.
- `AttendanceServiceTest`: verify duplicate detection using mock attendance repository.
- `AdminDashboardServiceTest`: verify count aggregations with mock repositories.

### Property-Based Tests (using QuickTheories or jqwik — jqwik is appropriate for Java 17 + JUnit 5)
Each property below corresponds to a Correctness Property above. Each test uses `@Property` from jqwik with minimum 100 tries.

- **Property 1**: Generate random `List<Faq>` with injected matching/non-matching records; verify search result subset.
- **Property 2**: Generate random `List<Faq>` with varied categories; verify category filter output.
- **Property 3**: Generate random `List<Payment>` with random `paidAt` timestamps; verify sort invariant.
- **Property 4**: For any booking mock transitioning to CONFIRMED, verify `Payment` with `SUCCESS` is saved exactly once.
- **Property 5**: For any event with N confirmed bookings (N ∈ [1..20]), verify exactly N `INITIATED` refunds created.
- **Property 6**: For any integer ID N ∈ [1..999999], verify `"U" + String.format("%06d", N)` format.
- **Property 7**: For any `String orgName` and integer ID, verify code matches `^O[A-Z]{3}[0-9]{6}$`.
- **Property 8**: For any queue entry with age > 10 min, verify expiry job sets `EXPIRED` and restores seats.
- **Property 9**: For any event creation, verify one `ApprovalRequest` created with `PENDING` and `event.status = PENDING_APPROVAL`.
- **Property 10**: For any confirmed booking, verify `qrCodePath != null && !qrCodePath.isBlank()`.
- **Property 11**: For any ticketId already checked in, second check-in always returns 409.
- **Property 12**: Mock `AuditLogRepository` to throw; verify primary operation still returns success response.

### Integration Tests
- Help Center FAQ list ordering: insert 5 FAQs across 3 categories, verify response order.
- Payment history pagination: insert 15 payments, verify page 0 returns 10 items sorted desc.
- SSE notification delivery: mock emitter, trigger `sendNotification()`, verify `emitter.send()` called.
- Email on event cancellation: mock `EmailService`, cancel event, verify `sendEventCancellationEmail` called N times for N bookings.

### Frontend Tests (Vitest + React Testing Library)
- `HelpCenter.jsx`: accordion toggle renders answer; search filters list.
- `QueueStatus.jsx`: countdown timer starts from 10:00 when status is `PAYMENT_PENDING`.
- `Profile.jsx`: "Detect My Location" button disabled when geolocation unavailable.
- `Admin/Dashboard.jsx`: spinner shown while loading; tables render after data resolves.

### Property Test Configuration
- Library: **jqwik 1.8.x** (already compatible with JUnit 5 in Spring Boot 3.2.5 test scope)
- Minimum tries per property: **100**
- Tag format: `Feature: advanced-event-booking-upgrade, Property {N}: {short title}`
- Each property test is a single `@Property` annotated method
- Optional sub-tasks in the task list are marked `*` and can be skipped for MVP
