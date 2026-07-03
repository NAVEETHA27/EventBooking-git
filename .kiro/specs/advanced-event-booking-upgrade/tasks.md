# Implementation Plan: Advanced Event Booking Upgrade

## Overview

This plan implements 13 advanced features on top of the existing Spring Boot 3.2.5 + React 18 college event booking system. The existing codebase already has entity models, service stubs, and partial controller implementations. Tasks proceed backend-first (schema → services → controllers) then frontend (API wiring → pages → UI), closing with integration checkpoints.

Language: **Java 17 (Spring Boot 3.2.5)** for backend, **JavaScript/JSX (React 18 + Vite)** for frontend.

---

## Tasks

- [ ] 1. Database Schema Completion
  - [~] 1.1 Update `sql/schema.sql` with all new column additions and tables
    - Add `user_code`, `city`, `state`, `country`, `pin_code`, `latitude`, `longitude` to `users` table
    - Add `organizer_code`, `city`, `state`, `country`, `pin_code` to `organizers` table
    - Add `approval_status` column to `events` table
    - Add `CREATE TABLE IF NOT EXISTS faq` with category, question, answer, active, indexes
    - Add `CREATE TABLE IF NOT EXISTS tutorial_videos` with all columns
    - Add `CREATE TABLE IF NOT EXISTS profile_locations` with owner_id, owner_type, lat/lng, address components
    - Verify `booking_queue`, `approval_requests`, `audit_logs`, `attendance` tables are complete
    - _Requirements: 4.1, 4.2, 5.2, 5.4, 1.1, 12.1, 13.1_

- [ ] 2. Backend — Help Center Completion
  - [~] 2.1 Complete `HelpCenterController.java` with all public and admin endpoints
    - Add `GET /help/faqs?search=&category=` (public, no auth), `GET /help/videos` (public)
    - Add `POST /admin/help/faqs`, `PUT /admin/help/faqs/{id}` secured to `ROLE_ADMIN`
    - Add `POST /admin/help/videos`, `PUT /admin/help/videos/{id}`, `DELETE /admin/help/videos/{id}` secured to `ROLE_ADMIN`
    - Add multipart file upload handling for video files using existing upload config
    - Verify all routes are configured in `SecurityConfig.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [ ]* 2.2 Write property tests for FAQ search and category filter (Property 1 & 2)
    - **Property 1: FAQ search always returns only matching records**
    - **Property 2: FAQ category filter returns only records with the matching category**
    - **Validates: Requirements 1.2, 1.3**

- [ ] 3. Backend — Unique ID Generation
  - [~] 3.1 Verify and harden `AuthService.java` User_Code and Organizer_Code generation
    - Confirm `User_Code` = `"U" + String.format("%06d", user.getId())` is assigned after `saveAndFlush`
    - Confirm `Organizer_Code` = `buildOrganizerCode(orgName, id)` producing `O{3-char}{06d}` is correct
    - Add null/missing code guard: if `userCode == null`, assign on profile fetch
    - Expose `userCode` in `AuthResponse.UserInfo` and `GET /users/me` response
    - Expose `organizerCode` in organizer profile response
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.8_

  - [ ]* 3.2 Write property tests for ID format correctness (Property 6 & 7)
    - **Property 6: For any user ID N, userCode equals "U" + String.format("%06d", N)**
    - **Property 7: For any orgName and ID, organizerCode matches regex `^O[A-Z]{3}[0-9]{6}$`**
    - **Validates: Requirements 4.1, 4.2**

- [ ] 4. Backend — Payment Management Completion
  - [~] 4.1 Extend `PaymentService.java` with complete history DTO and admin query
    - Update `history(Long userId, int page, int size)` to include `eventName` in response DTO
    - Create `PaymentHistoryResponse` DTO: `paymentId`, `transactionId`, `bookingId`, `eventName`, `amount`, `paymentStatus`, `paymentMethod`, `gatewayReference`, `paidAt`
    - Add `adminGetAll(PaymentStatus status, LocalDate from, LocalDate to, Pageable pageable)` method
    - _Requirements: 2.2, 2.3, 2.4_

  - [~] 4.2 Add payment controller endpoints
    - Add `GET /payments/history?page=0&size=10` to `PaymentController.java` secured to `ROLE_USER`
    - Add `GET /admin/payments?status=&from=&to=&page=0&size=20` secured to `ROLE_ADMIN`
    - Ensure booking confirmed event sets payment status to SUCCESS via `BookingService`
    - _Requirements: 2.2, 2.4, 2.5_

  - [ ]* 4.3 Write property test for payment history sort order (Property 3)
    - **Property 3: Payment history list is always sorted descending by paid_at**
    - **Validates: Requirements 2.2**

  - [ ]* 4.4 Write property test for booking-confirmation payment creation (Property 4)
    - **Property 4: Every booking transitioning to CONFIRMED creates exactly one SUCCESS payment**
    - **Validates: Requirements 2.5**

- [ ] 5. Backend — Refund Management Completion
  - [~] 5.1 Complete `RefundService.java` with auto-refund and admin workflow
    - Implement `autoInitiateForEvent(Long eventId)`: fetch all CONFIRMED bookings for event, create one Refund per booking with status `INITIATED`, call `NotificationService.sendNotification()` and `EmailService.sendRefundInitiatedEmail()` per user
    - Implement `requestRefund(Long bookingId, Long userId)`: validate booking ownership, transition to `REFUND_REQUESTED`
    - Implement `approveRefund(Long refundId, Long adminId)`: transition to `APPROVED`, send SSE + email
    - Implement `rejectRefund(Long refundId, Long adminId, String reason)`: transition to `REJECTED`, send SSE + email
    - _Requirements: 3.1, 3.2, 3.3, 3.6, 3.7, 3.8, 3.9_

  - [ ]* 5.2 Write property test for auto-refund count correctness (Property 5)
    - **Property 5: Cancelling an event with N confirmed bookings creates exactly N INITIATED refund records**
    - **Validates: Requirements 3.2**

  - [~] 5.3 Add refund controller endpoints
    - Add `POST /refunds/request` secured to `ROLE_USER`
    - Add `GET /refunds/my` secured to `ROLE_USER`
    - Add `GET /admin/refunds?status=&page=0` secured to `ROLE_ADMIN`
    - Add `PUT /admin/refunds/{id}/approve` and `PUT /admin/refunds/{id}/reject` secured to `ROLE_ADMIN`
    - Wire `EventService` cancellation method to call `refundService.autoInitiateForEvent()`
    - _Requirements: 3.3, 3.4, 3.5, 3.6_

- [~] 6. Checkpoint — Core backend services complete
  - Run `./mvnw test` to verify all existing and new backend unit tests pass. Ask the user if questions arise.

- [ ] 7. Backend — Registration Location Fields
  - [~] 7.1 Verify `AuthService.java` persists location fields during registration
    - Confirm `city`, `state`, `country`, `pinCode` are accepted in `UserRegisterRequest` and `OrganizerRegisterRequest` DTOs
    - Confirm `AuthService.registerUser()` and `registerOrganizer()` map these fields onto the entity
    - Add `POST /profile/location` endpoint in `UserProfileController` secured to authenticated users and organizers
    - Complete `ProfileLocationService.save(ownerId, ownerType, lat, lng, addressComponents)`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.7, 5.8, 5.10_

- [ ] 8. Backend — Booking Queue Hardening
  - [~] 8.1 Complete `BookingQueueService.java` with scheduled processing and expiry
    - Implement `enqueue(Long userId, Long eventId, int quantity)`: create `BookingQueueEntry` with `RECEIVED` status and current timestamp
    - Implement `@Scheduled(fixedDelay=500) processQueue()`: select oldest RECEIVED entry per event using `@Lock(PESSIMISTIC_WRITE)` on Event; decrement `available_seats`; set entry to `PAYMENT_PENDING` or `TICKETS_SOLD_OUT`; send SSE notification
    - Implement `@Scheduled(fixedDelay=60000) expireStaleEntries()`: find all `PAYMENT_PENDING` entries older than 10 minutes; set to `EXPIRED`; restore seats; send SSE
    - Implement `getStatus(String requestId)`: return current queue entry DTO
    - Add `GET /bookings/queue-status/{requestId}` to `BookingController` secured to `ROLE_USER`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9_

  - [ ]* 8.2 Write property test for queue expiry correctness (Property 8)
    - **Property 8: Any PAYMENT_PENDING entry older than 10 minutes is always set to EXPIRED by the expiry scheduler**
    - **Validates: Requirements 6.6**

- [ ] 9. Backend — Admin Approval System Completion
  - [~] 9.1 Create `ApprovalService.java` with full approval workflow
    - Implement `submitForApproval(Event event)`: create `ApprovalRequest` with `PENDING`; set `event.status = PENDING_APPROVAL`; notify admin via SSE
    - Implement `approve(Long approvalId, Long adminId)`: set `ApprovalRequest.status = APPROVED`; set `event.status = PUBLISHED`; notify organizer via SSE + email
    - Implement `reject(Long approvalId, Long adminId, String note)`: set `REJECTED`; set `event.status = REJECTED`; record note; notify organizer via SSE + email
    - Implement `requestModification(Long approvalId, Long adminId, String note)`: set `MODIFICATION_REQUESTED`; send note to organizer
    - Wire `EventService.createEvent()` to call `approvalService.submitForApproval(event)`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 9.2 Write property test for approval state invariant (Property 9)
    - **Property 9: Every event creation always results in exactly one PENDING ApprovalRequest and event.status == PENDING_APPROVAL**
    - **Validates: Requirements 7.1**

  - [~] 9.3 Add admin approval controller endpoints
    - Add `GET /admin/approvals?status=&page=0` secured to `ROLE_ADMIN`
    - Add `PUT /admin/approvals/{id}/approve` secured to `ROLE_ADMIN`
    - Add `PUT /admin/approvals/{id}/reject` secured to `ROLE_ADMIN`
    - Add `PUT /admin/approvals/{id}/request-modification` secured to `ROLE_ADMIN`
    - _Requirements: 7.2, 7.3, 7.4, 7.5_

- [ ] 10. Backend — Admin Dashboard Service
  - [~] 10.1 Complete `AdminDashboardService.java` with aggregate stats
    - Implement `getStats()`: run count queries for users, organizers, events by status, payments by status, refunds by status, pending approvals
    - Return `AdminDashboardStatsResponse` DTO with all counts
    - Add `GET /admin/dashboard` endpoint in `AdminController` secured to `ROLE_ADMIN`
    - Add `GET /admin/users?page=0`, `GET /admin/organizers?page=0`, `GET /admin/events?page=0` paginated list endpoints
    - Add `GET /admin/audit-logs?actorType=&action=&from=&to=&page=0` with filtering
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_

- [ ] 11. Backend — QR Code and Attendance
  - [~] 11.1 Create `QRCodeService.java` using ZXing
    - Add `com.google.zxing:core:3.5.3` and `com.google.zxing:javase:3.5.3` to `pom.xml`
    - Implement `generateQR(String ticketId)`: encode ticketId as 300x300 PNG; save to `{upload.dir}/qr/{ticketId}.png`; return file path
    - Wire into `BookingService.confirmBooking()`: call `qrCodeService.generateQR(booking.getTicketId())` and persist path to `booking.setQrCodePath(path)`
    - Add `GET /bookings/{bookingId}/qr-code` to `BookingController` secured to `ROLE_USER`; serve PNG binary response with `Content-Type: image/png`
    - _Requirements: 12.1, 12.2_

  - [ ]* 11.2 Write property test for QR code generation (Property 10)
    - **Property 10: For any confirmed booking, qrCodePath is non-null and non-blank after confirmation**
    - **Validates: Requirements 12.1**

  - [~] 11.3 Complete `AttendanceController.java` with check-in and list endpoints
    - Implement `POST /attendance/check-in` secured to `ROLE_ORGANIZER`: look up booking by `ticketId`; check for existing `Attendance` record; return 409 if duplicate, 400 if cancelled, 200 + create Attendance if valid
    - Implement `GET /attendance/event/{eventId}` secured to `ROLE_ORGANIZER`: return list of attendees for the event
    - Wire `AuditService.record()` on successful check-in
    - _Requirements: 12.3, 12.4, 12.5, 12.6, 12.7_

  - [ ]* 11.4 Write property test for duplicate check-in rejection (Property 11)
    - **Property 11: Any ticketId that has already been checked in always returns HTTP 409 on second scan**
    - **Validates: Requirements 12.5**

- [ ] 12. Backend — Email Service Completion
  - [~] 12.1 Add all missing email template methods to `EmailService.java`
    - Add `sendBookingConfirmationEmail(User user, Booking booking, String qrCodePath)`: HTML email with event details and QR code as inline attachment
    - Add `sendBookingCancellationEmail(User user, Booking booking)`
    - Add `sendPaymentSuccessEmail(User user, Payment payment)`
    - Add `sendPaymentFailureEmail(User user, Payment payment)`
    - Add `sendRefundInitiatedEmail(User user, Refund refund)`
    - Add `sendRefundStatusEmail(User user, Refund refund)` (covers APPROVED, REJECTED, COMPLETED)
    - Add `sendEventApprovalEmail(Organizer organizer, Event event, String status, String note)`
    - Add `sendEventCancellationEmail(User user, Event event)`
    - Annotate all methods `@Async`; wrap SMTP calls in try-catch and log at WARN on failure
    - _Requirements: 11.1 through 11.13_

- [ ] 13. Backend — Audit Service Wiring
  - [~] 13.1 Add `AuditService.record()` calls to all key service methods
    - `BookingService`: BOOKING_CREATED (include ticketId + eventId in details), BOOKING_CANCELLED
    - `PaymentService`: PAYMENT_RECORDED
    - `RefundService`: REFUND_REQUESTED, REFUND_STATUS_CHANGED
    - `EventService`: EVENT_CREATED, EVENT_PUBLISHED, EVENT_CANCELLED
    - `AttendanceService`: CHECK_IN_RECORDED
    - `ApprovalService`: APPROVAL_REVIEWED
    - _Requirements: 13.2, 13.5_

  - [ ]* 13.2 Write property test for audit isolation (Property 12)
    - **Property 12: When AuditLogRepository throws, the primary business operation still returns success**
    - **Validates: Requirements 13.6**

- [~] 14. Checkpoint — Full backend complete
  - Run `./mvnw test`. All tests must pass. Fix any compilation or integration errors. Ask the user if questions arise.

- [ ] 15. Frontend — Help Center Page
  - [~] 15.1 Complete `frontend/src/pages/HelpCenter.jsx`
    - Fetch `GET /help/faqs` and render FAQ accordion grouped by category using Framer Motion for animation
    - Add search input at top that calls `GET /help/faqs?search=<term>` and re-renders results
    - Add category chip filters that call `GET /help/faqs?category=<value>`
    - Fetch `GET /help/videos` and render video gallery; embed videos using `<video>` or iframe for external URLs
    - Use `btn-primary`, `input-field`, `shadow-card` CSS classes
    - _Requirements: 1.8, 1.9, 1.10_

- [ ] 16. Frontend — Payment History Page
  - [~] 16.1 Complete `frontend/src/pages/user/PaymentHistory.jsx`
    - Fetch `GET /payments/history?page=<n>&size=10` using react-query; render paginated table
    - Columns: Transaction ID, Event, Amount, Status (badge-colored by status value), Method, Date
    - Add status filter dropdown and date range picker that trigger new fetches without page reload
    - Protect route with `PrivateRoute` requiring `ROLE_USER`
    - _Requirements: 2.7, 2.8_

- [ ] 17. Frontend — Refund Tracking Page
  - [~] 17.1 Complete `frontend/src/pages/user/RefundTracking.jsx`
    - Fetch `GET /refunds/my` and render a table with columns: Event, Amount, Status badge, Request Date
    - Status badges: INITIATED (gray), REFUND_REQUESTED (blue), APPROVED (green), REJECTED (red), COMPLETED (green)
    - Protect route with `PrivateRoute` requiring `ROLE_USER`
    - _Requirements: 3.10_

- [ ] 18. Frontend — Queue Status Page
  - [~] 18.1 Complete `frontend/src/pages/user/QueueStatus.jsx`
    - Poll `GET /bookings/queue-status/{requestId}` every 3 seconds; render current status
    - When status is `PAYMENT_PENDING`, display 10-minute countdown timer
    - When status is `BOOKING_SUCCESSFUL`, show success state and link to bookings
    - When status is `TICKETS_SOLD_OUT` or `EXPIRED`, show error state with re-book link
    - _Requirements: 6.10_

- [ ] 19. Frontend — Registration Location Fields
  - [~] 19.1 Update `frontend/src/pages/auth/Register.jsx` with location fields and geolocation
    - Add city, state, country, pincode input fields to the registration form
    - Add "Detect My Location" button that calls `navigator.geolocation.getCurrentPosition()`
    - On geolocation success: call Nominatim reverse-geocoding API (`https://nominatim.openstreetmap.org/reverse`) to populate city, state, country, pincode
    - If geolocation unavailable/denied: disable button and show info message
    - _Requirements: 5.1, 5.5, 5.6, 5.9_

- [ ] 20. Frontend — Enhanced Profile Pages
  - [~] 20.1 Complete `frontend/src/pages/user/Profile.jsx` with unique ID, photo, location
    - Display `userCode` as non-editable field labeled "User ID"
    - Add profile photo upload section: POST to `PUT /users/profile-picture`; update displayed image on success without reload
    - Add address fields: city, state, country, pincode
    - Add "Change Password" section: current password, new password, confirm new password; POST to `POST /users/change-password`
    - Add "My Location" section: display stored coordinates; add "Detect My Location" button calling browser Geolocation + `POST /profile/location`
    - Show "No location saved" placeholder if no location exists
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.7, 9.8, 9.9_

  - [~] 20.2 Complete `frontend/src/pages/organizer/Profile.jsx` with unique ID, photo, location
    - Apply the same changes as 20.1 for organizer fields (organizerCode, organizerName, etc.)
    - Use `GET /organizer/profile` and `PUT /organizer/profile` endpoints
    - _Requirements: 9.6_

- [ ] 21. Frontend — Admin Dashboard
  - [~] 21.1 Complete `frontend/src/pages/admin/Dashboard.jsx` with all sections
    - Fetch `GET /admin/dashboard` and display metric cards: Total Users, Organizers, Events, Payments, Refunds, Pending Approvals
    - Add Users table section: fetch `GET /admin/users?page=0`; columns: User ID, Name, Email, Reg Date, Status
    - Add Organizers table section: fetch `GET /admin/organizers?page=0`; columns: Organizer ID, Name, Org Name, Email, Approval
    - Add Payments table section: fetch `GET /admin/payments?page=0`; columns: TXN ID, User, Event, Amount, Status, Date
    - Add Refunds section: fetch `GET /admin/refunds?status=REFUND_REQUESTED&page=0`; show approve/reject buttons
    - Add Audit Logs section: fetch `GET /admin/audit-logs?page=0&size=50`; columns: Timestamp, Actor, Action, Target, Details
    - Show Spinner in each section while loading
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9_

  - [~] 21.2 Complete `frontend/src/pages/admin/Approvals.jsx`
    - Fetch `GET /admin/approvals?status=PENDING&page=0` and render pending events table
    - Columns: Event Name, Organizer, Category, Date, Submitted At
    - Add action buttons: Approve, Reject, Request Modification (each opens a modal with notes input)
    - Show approval status on each row
    - _Requirements: 7.8_

- [ ] 22. Frontend — Notifications Page and Bell Icon
  - [~] 22.1 Complete `frontend/src/pages/user/Notifications.jsx`
    - Fetch `GET /notifications?page=0&size=20` and render notification list
    - Visual differentiation: unread (bold, blue left-border), read (normal)
    - Add "Mark All Read" button calling `POST /notifications/mark-read`
    - _Requirements: 10.7, 10.9_

  - [~] 22.2 Add SSE notification hook and bell icon in `Navbar.jsx`
    - Create `useNotifications` custom hook: open `EventSource` on auth, close on logout
    - On `notification` SSE event: increment unread badge count; show toast notification
    - Render bell icon with unread count badge in `Navbar.jsx`
    - _Requirements: 10.5, 10.6_

- [ ] 23. Frontend — Booking Detail QR Code
  - [~] 23.1 Update `frontend/src/pages/user/BookingDetail.jsx` to display QR code
    - Fetch `GET /bookings/{bookingId}/qr-code` and render as `<img>` tag
    - Show a "Download QR Code" link
    - _Requirements: 12.8_

- [~] 24. Final Checkpoint — Ensure all tests pass
  - Run `./mvnw test` (backend) and `npm run test -- --run` inside `frontend/` directory
  - Verify all admin routes require `ROLE_ADMIN`; verify QR code generation produces valid PNG files
  - Verify SSE connections open on login and close on logout
  - Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for full traceability
- Property tests use **jqwik 1.8.x** (add to `pom.xml` in test scope: `net.jqwik:jqwik:1.8.4`)
- ZXing for QR codes: add `com.google.zxing:core:3.5.3` and `com.google.zxing:javase:3.5.3` to `pom.xml`
- Nominatim reverse-geocoding is called from the browser frontend (no backend proxy needed)
- All email methods must be `@Async` — SMTP failures must not propagate to callers
- The audit log is append-only; no DELETE or UPDATE routes should be added for audit records
- `@Scheduled` methods in `BookingQueueService` require `@EnableScheduling` on the main application class

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1", "5.1", "7.1", "8.1", "9.1", "10.1", "11.1", "11.3", "12.1", "13.1"] },
    { "id": 2, "tasks": ["2.2", "3.2", "4.2", "4.3", "4.4", "5.2", "5.3", "8.2", "9.2", "9.3", "11.2", "11.4", "13.2"] },
    { "id": 3, "tasks": ["6", "14"] },
    { "id": 4, "tasks": ["15.1", "16.1", "17.1", "18.1", "19.1", "20.1", "20.2", "21.1", "21.2", "22.1", "22.2", "23.1"] },
    { "id": 5, "tasks": ["24"] }
  ]
}
```
