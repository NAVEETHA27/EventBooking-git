# Requirements Document

## Introduction

This document specifies 13 advanced feature upgrades for the existing Spring Boot 3.2.5 + React 18 college event booking system. The existing system (documented in the `college-event-booking-upgrade` spec) already implements JWT authentication, RBAC (USER/ORGANIZER/ADMIN roles), event CRUD, basic booking/payment models, SSE notifications infrastructure, audit logging, and a React frontend with Tailwind CSS. This upgrade builds on that foundation to deliver: FAQ & Help Center with video tutorials, complete payment and refund management lifecycle, unique human-readable IDs, enriched registration with location data, high-traffic booking queue with pessimistic locking, admin event approval workflow, an admin dashboard, enhanced profile pages, real-time SSE notifications, email notifications, QR-coded tickets with check-in deduplication, and an audit log UI.

## Glossary

- **System**: The full-stack college event booking application (Spring Boot 3.2.5 backend + React 18 frontend).
- **Help_Center**: The Spring `HelpCenterService` and `HelpCenterController` managing FAQs and tutorial videos.
- **FAQ**: A Frequently Asked Question record stored in the `faq` table with question, answer, and category fields.
- **Tutorial_Video**: A video resource record stored in the `tutorial_videos` table with title, description, URL, and category.
- **Payment_Service**: The Spring `PaymentService` that manages the full payment lifecycle for bookings.
- **Refund_Service**: The Spring `RefundService` that manages the refund request and approval workflow.
- **User_Code**: A unique human-readable identifier for users in format `U000001` (U + zero-padded 6-digit sequential number).
- **Organizer_Code**: A unique human-readable identifier for organizers in format `OABC000001` (O + 3-letter org prefix + zero-padded 6-digit ID).
- **Queue_Service**: The Spring service managing the `BookingQueueEntry` table for high-traffic event booking.
- **Approval_Service**: The Spring service managing the event approval workflow via the `ApprovalRequest` entity.
- **Admin_Dashboard**: The React page at `/admin/dashboard` showing aggregate statistics for users, organizers, events, payments, refunds, and approvals.
- **SSE_Emitter**: A Spring `SseEmitter` object held by the `NotificationService` per connected client session.
- **Notification_Service**: The Spring `NotificationService` managing SSE emitters and persisting notifications to MongoDB.
- **Email_Service**: The Spring `EmailService` sending transactional emails via Gmail SMTP.
- **QR_Code**: A per-ticket QR image encoding the booking `ticketId`, used for event check-in.
- **Attendance**: The Spring entity recording check-in events, used to prevent duplicate QR scans.
- **Audit_Log**: A record in the `audit_logs` table capturing actor, action, target, and timestamp for every key system action.
- **Profile_Location**: A Spring entity storing latitude/longitude and reverse-geocoded address components for a user or organizer.
- **Booking_Queue**: The `booking_queue` MySQL table holding queue entries with timestamps and pessimistic locking.
- **Payment_Timeout**: The 10-minute window after queue entry reaches `PAYMENT_PENDING` status before seats are auto-released.
- **Approval_Status**: The lifecycle of an event approval request: `PENDING` → `APPROVED` or `REJECTED` → organizer may resubmit after `MODIFICATION_REQUESTED`.

---

## Requirements

### Requirement 1: FAQ & Help Center with Video Tutorials

**User Story:** As a user or organizer, I want to browse FAQs and watch tutorial videos, so that I can resolve common issues without contacting support. As an admin, I want to manage FAQ entries and upload video tutorials, so that the help content stays current.

#### Acceptance Criteria

1. THE Help_Center SHALL expose `GET /help/faqs` returning all active FAQ records ordered by category ascending, then question ascending.
2. WHEN a search query parameter is provided to `GET /help/faqs?search=<term>`, THE Help_Center SHALL return FAQ records whose question or answer contains the search term (case-insensitive).
3. WHEN a category query parameter is provided to `GET /help/faqs?category=<value>`, THE Help_Center SHALL return only FAQ records matching that category.
4. THE Help_Center SHALL expose `GET /help/videos` returning all active tutorial video records ordered by `created_at` descending.
5. THE System SHALL expose `POST /admin/help/faqs` and `PUT /admin/help/faqs/{id}` restricted to `ROLE_ADMIN` for creating and updating FAQ entries.
6. THE System SHALL expose `POST /admin/help/videos`, `PUT /admin/help/videos/{id}`, and `DELETE /admin/help/videos/{id}` restricted to `ROLE_ADMIN` for managing tutorial videos.
7. WHEN an admin uploads a tutorial video file, THE System SHALL store the file under the configured upload directory and persist the file path in the `video_url` column.
8. THE Frontend SHALL render a Help Center page at `/help` accessible to all users showing FAQ accordions grouped by category and a video gallery section.
9. WHEN a user expands a FAQ accordion item, THE Frontend SHALL display the full answer text without a page reload.
10. THE Frontend SHALL display tutorial videos as playable embedded players or linked thumbnails within the video gallery section.

---

### Requirement 2: Complete Payment Management

**User Story:** As a user, I want to see my full payment history with status details, so that I can track all transactions. As an admin, I want to view and manage all payments across the platform.

#### Acceptance Criteria

1. THE Payment_Service SHALL support payment statuses: `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `REFUNDED`, `PARTIALLY_REFUNDED`.
2. THE System SHALL expose `GET /payments/history` secured to `ROLE_USER` returning a paginated list of the authenticated user's payment records ordered by `paid_at` descending.
3. EACH payment record in the response SHALL include: `paymentId`, `transactionId`, `bookingId`, `eventName`, `amount`, `paymentStatus`, `paymentMethod`, `gatewayReference`, `paidAt`.
4. THE System SHALL expose `GET /admin/payments` secured to `ROLE_ADMIN` returning a paginated list of all payment records with filtering by `paymentStatus` and date range.
5. WHEN a booking reaches `CONFIRMED` status, THE Payment_Service SHALL record a payment entry with `paymentStatus = SUCCESS` linked to that booking.
6. WHEN a booking is cancelled, THE Payment_Service SHALL update the linked payment record's `paymentStatus` to `REFUNDED` after refund completion.
7. THE Frontend SHALL render a Payment History page at `/payments/history` accessible only to `ROLE_USER`, showing a table with columns: Transaction ID, Event, Amount, Status (badge-colored), Method, Date.
8. THE Frontend SHALL provide date range and status filters on the Payment History page that re-fetch data without a full page reload.

---

### Requirement 3: Refund Management

**User Story:** As a user, I want to request a refund for a cancelled booking and track its status. As an admin, I want to review, approve, or reject refund requests, so that the refund workflow is transparent and auditable.

#### Acceptance Criteria

1. THE Refund_Service SHALL support refund statuses: `INITIATED`, `REFUND_REQUESTED`, `UNDER_VERIFICATION`, `APPROVED`, `PROCESSING`, `COMPLETED`, `REJECTED`, `FAILED`.
2. WHEN an event is cancelled by an organizer or admin, THE Refund_Service SHALL automatically create a refund record with status `INITIATED` for every `CONFIRMED` booking of that event.
3. THE System SHALL expose `POST /refunds/request` secured to `ROLE_USER` allowing a user to request a refund for a confirmed booking; the refund record SHALL transition to status `REFUND_REQUESTED`.
4. THE System SHALL expose `GET /refunds/my` secured to `ROLE_USER` returning all refund records for the authenticated user's bookings.
5. THE System SHALL expose `GET /admin/refunds` secured to `ROLE_ADMIN` returning a paginated list of all refund records filterable by status.
6. THE System SHALL expose `PUT /admin/refunds/{id}/approve` and `PUT /admin/refunds/{id}/reject` secured to `ROLE_ADMIN` to transition refund status to `APPROVED` or `REJECTED`.
7. WHEN a refund is approved, THE Notification_Service SHALL send an SSE notification and THE Email_Service SHALL send an email to the user confirming the refund approval.
8. WHEN a refund is rejected, THE Notification_Service SHALL send an SSE notification and THE Email_Service SHALL send an email to the user with the rejection reason.
9. WHEN an event is cancelled, THE Notification_Service SHALL send an SSE notification to all affected users informing them that a refund has been initiated.
10. THE Frontend SHALL render a Refund Tracking page at `/refunds/tracking` accessible to `ROLE_USER` showing each refund with status badge, amount, event name, and request date.
11. THE Frontend SHALL render an admin refunds management section within the Admin_Dashboard showing pending refunds with approve/reject action buttons.

---

### Requirement 4: Unique User and Organizer ID Generation

**User Story:** As a user, I want a unique public ID that identifies me on the platform. As an organizer, I want a unique ID that includes my organization prefix, so that I am easily identifiable in communications.

#### Acceptance Criteria

1. WHEN a new user is registered, THE Auth_Service SHALL generate a User_Code in the format `U` followed by the user's database `id` zero-padded to 6 digits (e.g., user with id 1 gets `U000001`).
2. WHEN a new organizer is registered, THE Auth_Service SHALL generate an Organizer_Code in the format `O` + 3-letter uppercase prefix derived from `organizationName` (non-alpha stripped, padded with `ORG` if shorter than 3 chars) + the organizer's database `id` zero-padded to 6 digits.
3. THE User_Code SHALL be stored in the `user_code` column of the `users` table and SHALL be immutable after initial assignment.
4. THE Organizer_Code SHALL be stored in the `organizer_code` column of the `organizers` table and SHALL be immutable after initial assignment.
5. THE System SHALL expose `GET /users/me` and `GET /organizer/profile` responses including the respective unique code field (`userCode` or `organizerCode`).
6. THE Frontend Profile_Page for users SHALL display the User_Code as a non-editable field labeled "User ID".
7. THE Frontend Profile_Page for organizers SHALL display the Organizer_Code as a non-editable field labeled "Organizer ID".
8. IF a user or organizer record exists without a code (legacy data), THEN THE System SHALL generate and assign a code on the next profile fetch.

---

### Requirement 5: Improved Registration with Location Data

**User Story:** As a new user or organizer, I want to provide my city, state, country, and pincode during registration, and optionally detect my location via Google Maps, so that my profile is location-enriched from the start.

#### Acceptance Criteria

1. THE registration form for users SHALL include fields: `city`, `state`, `country`, `pincode`, each accepting free-text input.
2. THE `users` table SHALL have columns `city` (VARCHAR 100), `state` (VARCHAR 100), `country` (VARCHAR 100), `pin_code` (VARCHAR 20).
3. THE registration form for organizers SHALL include the same location fields: `city`, `state`, `country`, `pincode`.
4. THE `organizers` table SHALL have the same location columns as the `users` table.
5. THE Frontend registration page SHALL include a "Detect My Location" button that calls the browser Geolocation API to obtain latitude and longitude.
6. WHEN the browser Geolocation API returns coordinates, THE Frontend SHALL call a reverse-geocoding lookup to populate the city, state, country, and pincode fields automatically.
7. THE System SHALL expose `POST /profile/location` secured to authenticated users and organizers, accepting `latitude` and `longitude`, and persisting the coordinates in the `profile_locations` table.
8. WHEN `POST /profile/location` is called, THE System SHALL respond with the stored location record including the persisted coordinates.
9. IF the browser Geolocation API is unavailable or denied, THEN THE Frontend SHALL disable the "Detect My Location" button and display an informational message without crashing.
10. THE Auth_Service SHALL accept and persist location fields (`city`, `state`, `country`, `pinCode`) during both user and organizer registration.

---

### Requirement 6: High-Traffic Booking Queue

**User Story:** As a user booking tickets for a high-demand event, I want to be queued fairly by request time, so that I am not unfairly displaced by concurrent requests. As a system, I want seats to be released automatically if payment is not completed within 10 minutes.

#### Acceptance Criteria

1. WHEN a user submits a booking request, THE Queue_Service SHALL create a `BookingQueueEntry` with status `RECEIVED` and record the `request_timestamp` at the moment of receipt.
2. THE Queue_Service SHALL process queue entries in ascending `request_timestamp` order using pessimistic locking (`SELECT … FOR UPDATE`) on the `events.available_seats` column to prevent double-booking.
3. WHEN a queue entry begins processing, THE Queue_Service SHALL transition its status from `RECEIVED` to `PROCESSING`.
4. WHEN available seats are confirmed for the requested quantity, THE Queue_Service SHALL decrement `events.available_seats` atomically and transition the entry status to `PAYMENT_PENDING`.
5. WHEN `events.available_seats` is insufficient for the requested quantity, THE Queue_Service SHALL set entry status to `TICKETS_SOLD_OUT` and notify the user via SSE.
6. WHEN a queue entry has been in `PAYMENT_PENDING` status for more than 10 minutes without a completed payment, THE System SHALL transition the entry to `EXPIRED`, restore the reserved seats to `events.available_seats`, and notify the user via SSE.
7. WHEN payment is completed successfully, THE Queue_Service SHALL transition the entry to `BOOKING_SUCCESSFUL` and create the booking record.
8. WHEN payment fails, THE Queue_Service SHALL transition the entry to `PAYMENT_FAILED`, restore the reserved seats, and notify the user via SSE.
9. THE System SHALL expose `GET /bookings/queue-status/{requestId}` secured to `ROLE_USER` returning the current status of a queue entry.
10. THE Frontend SHALL render a Queue Status page at `/queue-status` showing the current queue status, position indicator, and a countdown timer when in `PAYMENT_PENDING` state.

---

### Requirement 7: Admin Approval System for Events

**User Story:** As an organizer, I want to submit my event for admin approval before it goes live, so that only vetted events are published. As an admin, I want to approve, reject, or request modifications on submitted events.

#### Acceptance Criteria

1. WHEN an organizer submits a new event, THE Approval_Service SHALL create an `ApprovalRequest` record with status `PENDING` linked to the event, and THE Event entity's `status` SHALL be set to `PENDING_APPROVAL`.
2. THE System SHALL expose `GET /admin/approvals` secured to `ROLE_ADMIN` returning a paginated list of approval requests filterable by status.
3. THE System SHALL expose `PUT /admin/approvals/{id}/approve` secured to `ROLE_ADMIN` which sets `ApprovalRequest.status = APPROVED`, sets `Event.status = APPROVED`, and notifies the organizer via SSE and email.
4. THE System SHALL expose `PUT /admin/approvals/{id}/reject` secured to `ROLE_ADMIN` which sets `ApprovalRequest.status = REJECTED`, sets `Event.status = REJECTED`, records the `reviewNote`, and notifies the organizer via SSE and email.
5. THE System SHALL expose `PUT /admin/approvals/{id}/request-modification` secured to `ROLE_ADMIN` which sets `ApprovalRequest.status = MODIFICATION_REQUESTED` and sends the `reviewNote` to the organizer via SSE and email.
6. WHEN an approval is granted, THE Approval_Service SHALL additionally set `Event.status = PUBLISHED` and make the event visible to users.
7. WHEN an organizer updates a rejected event and resubmits, THE Approval_Service SHALL create a new `ApprovalRequest` record with status `PENDING` and reset `Event.status = PENDING_APPROVAL`.
8. THE Frontend shall render an Approvals management page at `/admin/approvals` showing pending events in a table with approve/reject/modify action buttons and a notes input.
9. THE Frontend Organizer Dashboard SHALL display the approval status of each event alongside its listing.

---

### Requirement 8: Admin Dashboard

**User Story:** As an admin, I want a unified dashboard showing platform-wide statistics, so that I can monitor system health and manage all entities from one place.

#### Acceptance Criteria

1. THE Admin_Dashboard SHALL display aggregate metric cards for: total registered users, total organizers, total events (by status), total payments (by status), total refunds (by status), and pending approvals count.
2. THE System SHALL expose `GET /admin/dashboard` secured to `ROLE_ADMIN` returning a JSON object with all the above aggregate counts plus a list of recent activity (last 10 audit log entries).
3. THE Admin_Dashboard SHALL display a Users section with a paginated table showing: User ID (User_Code), name, email, registration date, and a status badge (active/locked).
4. THE Admin_Dashboard SHALL display an Organizers section with a paginated table showing: Organizer ID (Organizer_Code), organizer name, organization name, email, and approval status.
5. THE Admin_Dashboard SHALL display an Events section with a paginated table showing: event name, organizer, category, date, status badge, and an action link to the approval detail.
6. THE Admin_Dashboard SHALL display a Payments section with a paginated table showing: transaction ID, user, event, amount, status badge, and date.
7. THE Admin_Dashboard SHALL display a Refunds section with a paginated table showing: refund ID, user, event, amount, status badge, request date, and approve/reject action buttons.
8. THE Admin_Dashboard SHALL display an Audit Logs section showing the last 50 audit entries with actor, action, target, and timestamp columns.
9. WHEN data for any Admin_Dashboard section is loading, THE Admin_Dashboard SHALL display a Spinner in place of that section's content.
10. THE Frontend SHALL protect the entire `/admin/**` route tree with a `PrivateRoute` that requires `ROLE_ADMIN`.

---

### Requirement 9: Enhanced Profile Page

**User Story:** As a user or organizer, I want a rich profile page showing my unique ID, photo, full address, and location on a map, so that my account information is comprehensive and easy to manage.

#### Acceptance Criteria

1. THE user Profile_Page SHALL display the User_Code field as a non-editable labeled "User ID".
2. THE user Profile_Page SHALL display a profile photo with an upload button that sends a multipart form POST to `PUT /users/profile-picture` and updates the displayed image without full page reload.
3. THE user Profile_Page SHALL display and allow editing of: name, email (read-only), phone, address, city, state, country, pincode.
4. THE user Profile_Page SHALL display a "Change Password" section with current password, new password, and confirm new password fields.
5. WHEN the user submits the change password form, THE System SHALL verify the current password before updating to the new password hash, and return HTTP 400 with message `"Current password is incorrect"` if verification fails.
6. THE organizer Profile_Page SHALL follow the same display and editing rules as criteria 1–5 for organizer fields, showing Organizer_Code as non-editable.
7. THE Profile_Page SHALL include a "My Location" section displaying the user's last saved coordinates as a static Google Maps embed or coordinate readout.
8. WHEN the user clicks "Detect My Location" on the profile page, THE Frontend SHALL invoke the browser Geolocation API and call `POST /profile/location` with the obtained coordinates.
9. IF the `profile_locations` table contains no entry for the authenticated user, THE Profile_Page SHALL display a "No location saved" placeholder in the location section.

---

### Requirement 10: Real-Time Notifications via SSE

**User Story:** As a user or organizer, I want to receive real-time push notifications in the browser for key events, so that I am immediately informed without polling.

#### Acceptance Criteria

1. THE Notification_Service SHALL maintain SSE connections per authenticated user/organizer using `SseEmitter` objects stored in a `ConcurrentHashMap` keyed by `<recipientType>:<recipientId>`.
2. THE System SHALL expose `GET /notifications/stream` secured to authenticated principals, returning an SSE `text/event-stream` response that keeps the connection open for up to 30 minutes.
3. WHEN the SSE connection is established, THE Notification_Service SHALL send an initial `connected` event with payload `"ok"`.
4. THE Notification_Service SHALL send real-time SSE events for the following triggers: booking confirmed, booking cancelled, payment success, payment failed, refund status change, event approval status change, event cancellation, queue status change, and account login alert.
5. THE Frontend SHALL establish an `EventSource` connection to `/api/notifications/stream` on user/organizer login and close it on logout.
6. WHEN an SSE notification is received, THE Frontend notification bell icon SHALL increment its unread badge count and display a toast message with the notification title.
7. THE System SHALL expose `GET /notifications` returning a paginated list of the authenticated user's notifications ordered by `created_at` descending.
8. THE System SHALL expose `POST /notifications/mark-read` accepting an array of notification IDs and marking those records as read.
9. THE Frontend Notifications page at `/notifications` SHALL display all notifications with read/unread visual differentiation and a "Mark All Read" button.
10. WHEN an SSE emitter times out or encounters an error, THE Notification_Service SHALL remove it from the active emitters map to prevent memory leaks.

---

### Requirement 11: Email Notifications for All Lifecycle Events

**User Story:** As a user or organizer, I want to receive transactional emails for all key system actions, so that I am informed even when not actively using the application.

#### Acceptance Criteria

1. THE Email_Service SHALL send a welcome email to users upon successful registration.
2. THE Email_Service SHALL send a welcome email to organizers upon successful registration.
3. THE Email_Service SHALL send an email verification OTP email during user and organizer registration.
4. THE Email_Service SHALL send a booking confirmation email to users when a booking is confirmed, including event name, date, venue, ticket ID, and QR code attachment.
5. THE Email_Service SHALL send a booking cancellation email to users when their booking is cancelled.
6. THE Email_Service SHALL send a payment success email to users when payment is recorded as `SUCCESS`.
7. THE Email_Service SHALL send a payment failure email to users when payment status is `FAILED`.
8. THE Email_Service SHALL send a refund initiated email when a refund record is created.
9. THE Email_Service SHALL send a refund status update email when a refund transitions to `APPROVED`, `REJECTED`, or `COMPLETED`.
10. THE Email_Service SHALL send an event approval notification email to the organizer when their event is `APPROVED`, `REJECTED`, or flagged for `MODIFICATION_REQUESTED`.
11. THE Email_Service SHALL send an event cancellation notification email to all affected booking users when an event is cancelled.
12. THE Email_Service SHALL send an account locked notification email when a user's account is locked after 5 failed login attempts.
13. IF any email delivery fails, THEN THE Email_Service SHALL log the failure at WARN level and SHALL NOT propagate the exception to interrupt the primary transaction.

---

### Requirement 12: QR Code per Ticket with Duplicate Check-In Prevention

**User Story:** As a user, I want a QR code on my ticket that can be scanned at the venue, so that check-in is contactless. As an organizer, I want duplicate scans to be rejected, so that ticket fraud is prevented.

#### Acceptance Criteria

1. WHEN a booking is confirmed, THE System SHALL generate a QR code image encoding the booking's `ticketId` and store the image file path in `bookings.qr_code_path`.
2. THE System SHALL expose `GET /bookings/{bookingId}/qr-code` secured to `ROLE_USER` returning the QR code image binary (PNG) for the booking owned by the authenticated user.
3. THE System SHALL expose `POST /attendance/check-in` secured to `ROLE_ORGANIZER` accepting `{ ticketId: String }` and performing the check-in scan.
4. WHEN a check-in scan is received for a `ticketId` not yet used, THE System SHALL create an `Attendance` record, mark the booking as checked in, and return HTTP 200 with `{ "status": "CHECKED_IN", "ticketId": "...", "userName": "...", "eventName": "..." }`.
5. WHEN a check-in scan is received for a `ticketId` that has already been used (duplicate), THE System SHALL return HTTP 409 with `{ "status": "DUPLICATE_SCAN", "message": "This ticket has already been used." }`.
6. WHEN a check-in scan is received for a `ticketId` belonging to a cancelled booking, THE System SHALL return HTTP 400 with `{ "status": "INVALID_TICKET", "message": "This booking has been cancelled." }`.
7. THE System SHALL expose `GET /attendance/event/{eventId}` secured to `ROLE_ORGANIZER` returning the full attendance list for a given event, showing each attendee's name, ticket ID, and check-in timestamp.
8. THE Frontend Booking Detail page SHALL display the QR code image fetched from `GET /bookings/{bookingId}/qr-code`.
9. THE Email_Service SHALL attach the QR code image to the booking confirmation email (Requirement 11.4).

---

### Requirement 13: Audit Log for All Key Actions

**User Story:** As an admin, I want a comprehensive audit trail of all key system actions, so that I can investigate issues and ensure accountability.

#### Acceptance Criteria

1. THE Audit_Log table SHALL record every key system action with fields: `id`, `actor_id`, `actor_type` (USER/ORGANIZER/ADMIN/SYSTEM), `action` (string label), `target_type`, `target_id`, `details` (text), `created_at`.
2. THE AuditService SHALL record audit entries asynchronously for all of the following actions: user registration, organizer registration, user login, organizer login, booking created, booking cancelled, payment recorded, refund requested, refund status changed, event created, event published, event cancelled, event approval reviewed, admin login, account locked, check-in recorded.
3. THE System SHALL expose `GET /admin/audit-logs` secured to `ROLE_ADMIN` returning a paginated list of audit log entries filterable by `actor_type`, `action`, and date range, ordered by `created_at` descending.
4. THE Admin_Dashboard audit logs section SHALL fetch from `GET /admin/audit-logs?page=0&size=50` and render a table with columns: Timestamp, Actor (ID + type), Action, Target (type + ID), Details.
5. WHEN an audit log entry is written for a `BOOKING_CREATED` action, THE AuditService SHALL include the `ticketId` and `eventId` in the `details` field.
6. IF the audit log write fails (e.g., database unavailable), THEN THE AuditService SHALL log the failure at WARN level and SHALL NOT propagate the exception or disrupt the primary operation.
7. THE Audit_Log SHALL be append-only; no update or delete operations SHALL be exposed on audit log records.
