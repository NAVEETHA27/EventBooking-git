-- ╔══════════════════════════════════════════════════════════════════════╗
--  Online Event Ticket Booking System – MySQL Schema
--  Database: event_booking_db
-- ╚══════════════════════════════════════════════════════════════════════╝

CREATE DATABASE IF NOT EXISTS event_booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE event_booking_db;

-- ─── USERS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_code             VARCHAR(20)   UNIQUE,
    name                  VARCHAR(120)  NOT NULL,
    email                 VARCHAR(160)  NOT NULL UNIQUE,
    password_hash         VARCHAR(255)  NOT NULL,
    phone                 VARCHAR(20),
    address               VARCHAR(300),
    pin_code              VARCHAR(20),
    organization_name     VARCHAR(160),
    city                  VARCHAR(100),
    state                 VARCHAR(100),
    country               VARCHAR(100),
    date_of_birth         DATE,
    gender                VARCHAR(20),
    profile_picture       VARCHAR(300),
    email_verified        BOOLEAN       NOT NULL DEFAULT FALSE,
    verification_token    VARCHAR(120),
    reset_password_token  VARCHAR(120),
    reset_token_expiry    DATETIME,
    account_locked        BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_login_attempts INT           NOT NULL DEFAULT 0,
    role                  VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at            DATETIME,
    updated_at            DATETIME,
    INDEX idx_user_email        (email),
    INDEX idx_user_public_id    (user_code),
    INDEX idx_user_verified     (email_verified)
);

-- ─── ORGANIZERS ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS organizers (
    id                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    organizer_code        VARCHAR(30)   UNIQUE,
    organizer_name        VARCHAR(120)  NOT NULL,
    organization_name     VARCHAR(160)  NOT NULL,
    email                 VARCHAR(160)  NOT NULL UNIQUE,
    password_hash         VARCHAR(255)  NOT NULL,
    phone                 VARCHAR(20),
    address               VARCHAR(300),
    pin_code              VARCHAR(20),
    city                  VARCHAR(100),
    state                 VARCHAR(100),
    country               VARCHAR(100),
    organization_logo     VARCHAR(300),
    website               VARCHAR(200),
    description           TEXT,
    email_verified        BOOLEAN       NOT NULL DEFAULT FALSE,
    verification_token    VARCHAR(120),
    reset_password_token  VARCHAR(120),
    reset_token_expiry    DATETIME,
    is_approved           BOOLEAN       NOT NULL DEFAULT TRUE,
    role                  VARCHAR(20)   NOT NULL DEFAULT 'ORGANIZER',
    created_at            DATETIME,
    updated_at            DATETIME,
    INDEX idx_org_email (email),
    INDEX idx_org_public_id (organizer_code)
);

CREATE TABLE IF NOT EXISTS profile_locations (
    id           BIGINT         PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT         NULL,
    organizer_id BIGINT         NULL,
    address      VARCHAR(300),
    street       VARCHAR(120),
    area         VARCHAR(120),
    city         VARCHAR(100),
    district     VARCHAR(100),
    state        VARCHAR(100),
    country      VARCHAR(100),
    pin_code     VARCHAR(20),
    latitude     DECIMAL(10,7),
    longitude    DECIMAL(10,7),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (organizer_id) REFERENCES organizers(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_profile_location_user (user_id),
    UNIQUE INDEX idx_profile_location_organizer (organizer_id)
);

-- ─── EVENTS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS events (
    id                BIGINT          PRIMARY KEY AUTO_INCREMENT,
    organizer_id      BIGINT          NOT NULL,
    event_name        VARCHAR(200)    NOT NULL,
    description       TEXT,
    category          VARCHAR(80)     NOT NULL,
    event_type        VARCHAR(60)     NOT NULL,
    event_banner      VARCHAR(300),
    event_images      TEXT,
    event_date        DATE            NOT NULL,
    event_time        TIME            NOT NULL,
    end_date          DATE,
    end_time          TIME,
    venue_name        VARCHAR(200),
    location          VARCHAR(300),
    google_maps_url   VARCHAR(500),
    ticket_price      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_seats       INT             NOT NULL,
    available_seats   INT             NOT NULL,
    status            VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    visibility        VARCHAR(20)     NOT NULL DEFAULT 'PUBLIC',
    is_featured       BOOLEAN         NOT NULL DEFAULT FALSE,
    tags              VARCHAR(400),
    organizer_details TEXT,
    created_at        DATETIME,
    updated_at        DATETIME,
    FOREIGN KEY (organizer_id) REFERENCES organizers(id) ON DELETE CASCADE,
    INDEX idx_event_status    (status),
    INDEX idx_event_date      (event_date),
    INDEX idx_event_type      (event_type),
    INDEX idx_event_category  (category),
    INDEX idx_event_organizer (organizer_id)
);

-- ─── BOOKINGS ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id                   BIGINT         PRIMARY KEY AUTO_INCREMENT,
    ticket_id            VARCHAR(50)    NOT NULL UNIQUE,
    user_id              BIGINT         NOT NULL,
    event_id             BIGINT         NOT NULL,
    quantity             INT            NOT NULL,
    total_amount         DECIMAL(10,2)  NOT NULL,
    booking_status       VARCHAR(30)    NOT NULL DEFAULT 'CONFIRMED',
    qr_code_path         VARCHAR(300),
    cancellation_reason  VARCHAR(500),
    cancelled_at         DATETIME,
    booked_at            DATETIME,
    updated_at           DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_booking_ticket (ticket_id),
    INDEX idx_booking_user   (user_id),
    INDEX idx_booking_event  (event_id),
    INDEX idx_booking_status (booking_status)
);

-- ─── PAYMENTS ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id                BIGINT         PRIMARY KEY AUTO_INCREMENT,
    transaction_id    VARCHAR(60)    NOT NULL UNIQUE,
    booking_id        BIGINT         NOT NULL,
    amount            DECIMAL(10,2)  NOT NULL,
    payment_status    VARCHAR(50)    NOT NULL,
    payment_method    VARCHAR(40),
    gateway_reference VARCHAR(120),
    gateway_order_id   VARCHAR(120),
    failure_reason    VARCHAR(300),
    paid_at           DATETIME,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    INDEX idx_payment_txn     (transaction_id),
    INDEX idx_payment_booking (booking_id),
    INDEX idx_payment_status  (payment_status)
);

-- ─── TRANSACTIONS (NEW) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    txn_id          VARCHAR(60)     NOT NULL UNIQUE,
    amount          DECIMAL(10,2)   NOT NULL,
    payment_status  VARCHAR(20)     NOT NULL,
    payment_date    DATETIME,
    user_id         BIGINT          NOT NULL,
    event_id        BIGINT          NOT NULL,
    created_at      DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_txn_id     (txn_id),
    INDEX idx_txn_user          (user_id),
    INDEX idx_txn_event         (event_id),
    INDEX idx_txn_status        (payment_status)
);

-- ─── EVENTS TABLE — add new columns if not present ────────────────────
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS college_name          VARCHAR(200)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS department_name       VARCHAR(150)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS has_certificate       BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS registration_deadline DATE          DEFAULT NULL;

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS whatsapp_group_link VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS whatsapp_contact_number VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS venue_latitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS venue_longitude DOUBLE NULL,
    ADD COLUMN IF NOT EXISTS estimated_travel_time VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS cab_estimate VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS nearby_hotels TEXT NULL,
    ADD COLUMN IF NOT EXISTS nearby_restaurants TEXT NULL,
    ADD COLUMN IF NOT EXISTS emergency_contacts TEXT NULL,
    ADD COLUMN IF NOT EXISTS tea_coffee_provided BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS special_diet VARCHAR(300) NULL,
    ADD COLUMN IF NOT EXISTS boys_hostel_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS girls_hostel_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hotel_tieup_available BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS accommodation_check_in VARCHAR(80) NULL,
    ADD COLUMN IF NOT EXISTS accommodation_check_out VARCHAR(80) NULL,
    ADD COLUMN IF NOT EXISTS accommodation_contact_person VARCHAR(160) NULL,
    ADD COLUMN IF NOT EXISTS session_schedule TEXT NULL,
    ADD COLUMN IF NOT EXISTS speaker_list TEXT NULL,
    ADD COLUMN IF NOT EXISTS live_announcements TEXT NULL;

-- ─── REFUNDS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refunds (
    id               BIGINT         PRIMARY KEY AUTO_INCREMENT,
    payment_id       BIGINT         NOT NULL,
    refund_amount    DECIMAL(10,2)  NOT NULL,
    refund_status    VARCHAR(30)    NOT NULL DEFAULT 'INITIATED',
    reason           VARCHAR(400),
    refund_reference VARCHAR(100),
    refund_date      DATETIME,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    INDEX idx_refund_payment (payment_id),
    INDEX idx_refund_status  (refund_status)
);
-- 2026-07-02: ticket lifecycle, multi-participant registration, and bonafide uploads
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS ticket_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS expired_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS attendance_status VARCHAR(30) NOT NULL DEFAULT 'NOT_ATTENDED',
    ADD COLUMN IF NOT EXISTS check_in_time DATETIME NULL,
    ADD COLUMN IF NOT EXISTS checked_in_by BIGINT NULL,
    ADD COLUMN IF NOT EXISTS certificate_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD INDEX IF NOT EXISTS idx_booking_ticket_status (ticket_status);

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS authorized_document_url VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    department VARCHAR(150),
    college VARCHAR(200),
    created_at DATETIME,
    CONSTRAINT fk_participant_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT uk_participant_event_email UNIQUE (event_id, email),
    INDEX idx_participant_booking (booking_id),
    INDEX idx_participant_event_email (event_id, email)
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(50) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL,
    checked_in_at DATETIME NOT NULL,
    scanned_by_organizer_id BIGINT,
    CONSTRAINT fk_attendance_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    UNIQUE INDEX idx_attendance_ticket (ticket_id),
    INDEX idx_attendance_booking (booking_id)
);

UPDATE users SET user_code = CONCAT('USR', LPAD(id, 4, '0')) WHERE user_code IS NULL OR user_code = '';
UPDATE organizers SET organizer_code = CONCAT('ORG', LPAD(id, 4, '0')) WHERE organizer_code IS NULL OR organizer_code = '';

-- ════════════════════════════════════════════════════════════════════
-- AI PLATFORM UPGRADE — New Tables (2026)
-- ════════════════════════════════════════════════════════════════════

-- Ratings & Reviews
CREATE TABLE IF NOT EXISTS event_ratings (
    id                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    event_id                 BIGINT        NOT NULL,
    user_id                  BIGINT        NOT NULL,
    booking_id               BIGINT,
    overall_rating           INT           NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    speaker_rating           INT           CHECK (speaker_rating BETWEEN 1 AND 5),
    venue_rating             INT           CHECK (venue_rating BETWEEN 1 AND 5),
    food_rating              INT           CHECK (food_rating BETWEEN 1 AND 5),
    organization_rating      INT           CHECK (organization_rating BETWEEN 1 AND 5),
    time_management_rating   INT           CHECK (time_management_rating BETWEEN 1 AND 5),
    content_quality_rating   INT           CHECK (content_quality_rating BETWEEN 1 AND 5),
    value_for_money_rating   INT           CHECK (value_for_money_rating BETWEEN 1 AND 5),
    review_text              TEXT,
    photo_urls               TEXT,
    is_verified_attendance   BOOLEAN       NOT NULL DEFAULT FALSE,
    moderation_status        VARCHAR(20)   NOT NULL DEFAULT 'APPROVED',
    moderation_note          VARCHAR(500),
    is_fake_flagged          BOOLEAN       NOT NULL DEFAULT FALSE,
    sentiment                VARCHAR(20),
    sentiment_score          DECIMAL(5,4),
    is_testimonial_candidate BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at               DATETIME,
    updated_at               DATETIME,
    FOREIGN KEY (event_id)   REFERENCES events(id)    ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)  ON DELETE SET NULL,
    UNIQUE INDEX uk_rating_user_event (user_id, event_id),
    INDEX idx_rating_event   (event_id),
    INDEX idx_rating_user    (user_id)
);

-- Certificates
CREATE TABLE IF NOT EXISTS certificates (
    id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    certificate_id   VARCHAR(50)   NOT NULL UNIQUE,
    event_id         BIGINT        NOT NULL,
    user_id          BIGINT        NOT NULL,
    recipient_name   VARCHAR(120)  NOT NULL,
    college_name     VARCHAR(200),
    department_name  VARCHAR(150),
    event_name       VARCHAR(200)  NOT NULL,
    issued_at        DATETIME,
    pdf_url          VARCHAR(500),
    qr_code_url      VARCHAR(500),
    email_sent       BOOLEAN       NOT NULL DEFAULT FALSE,
    email_sent_at    DATETIME,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    INDEX idx_cert_event (event_id),
    INDEX idx_cert_user  (user_id),
    UNIQUE INDEX idx_cert_code (certificate_id)
);

-- Sentiment Analysis
CREATE TABLE IF NOT EXISTS sentiment_analyses (
    id                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    event_id                 BIGINT        NOT NULL UNIQUE,
    positive_count           INT           NOT NULL DEFAULT 0,
    neutral_count            INT           NOT NULL DEFAULT 0,
    negative_count           INT           NOT NULL DEFAULT 0,
    satisfaction_score       DECIMAL(4,2)  NOT NULL DEFAULT 0,
    strengths                TEXT,
    weaknesses               TEXT,
    improvement_suggestions  TEXT,
    improvement_report       TEXT,
    ai_testimonial           TEXT,
    total_reviews_analyzed   INT           NOT NULL DEFAULT 0,
    created_at               DATETIME,
    updated_at               DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- Organizer Scores
CREATE TABLE IF NOT EXISTS organizer_scores (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    organizer_id      BIGINT        NOT NULL UNIQUE,
    average_rating    DECIMAL(4,2)  NOT NULL DEFAULT 0,
    total_events      INT           NOT NULL DEFAULT 0,
    completed_events  INT           NOT NULL DEFAULT 0,
    total_registrations BIGINT      NOT NULL DEFAULT 0,
    total_attendance  BIGINT        NOT NULL DEFAULT 0,
    attendance_rate   DECIMAL(5,2)  NOT NULL DEFAULT 0,
    refund_ratio      DECIMAL(5,2)  NOT NULL DEFAULT 0,
    cancellation_rate DECIMAL(5,2)  NOT NULL DEFAULT 0,
    overall_score     DECIMAL(5,2)  NOT NULL DEFAULT 0,
    badge             VARCHAR(30)   NOT NULL DEFAULT 'NONE',
    is_featured       BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at        DATETIME,
    FOREIGN KEY (organizer_id) REFERENCES organizers(id) ON DELETE CASCADE
);

-- User Interests / Preferences
CREATE TABLE IF NOT EXISTS user_interests (
    id                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id              BIGINT        NOT NULL UNIQUE,
    skills               VARCHAR(500),
    interests            VARCHAR(500),
    department           VARCHAR(150),
    college              VARCHAR(200),
    year_of_study        VARCHAR(20),
    favorite_categories  VARCHAR(300),
    preferred_event_type VARCHAR(20),
    career_goal          VARCHAR(300),
    updated_at           DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Event Recommendations Cache
CREATE TABLE IF NOT EXISTS event_recommendations (
    id                       BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id                  BIGINT        NOT NULL,
    event_id                 BIGINT        NOT NULL,
    recommendation_category  VARCHAR(50),
    match_score              DOUBLE        NOT NULL DEFAULT 0,
    reason                   VARCHAR(500),
    rank_position            INT           NOT NULL DEFAULT 0,
    created_at               DATETIME,
    expires_at               DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_rec_user     (user_id),
    INDEX idx_rec_event    (event_id),
    INDEX idx_rec_category (recommendation_category)
);

-- User Gamification Levels
CREATE TABLE IF NOT EXISTS user_levels (
    id                   BIGINT  PRIMARY KEY AUTO_INCREMENT,
    user_id              BIGINT  NOT NULL UNIQUE,
    total_xp             INT     NOT NULL DEFAULT 0,
    current_level        INT     NOT NULL DEFAULT 1,
    events_attended      INT     NOT NULL DEFAULT 0,
    events_registered    INT     NOT NULL DEFAULT 0,
    certificates_earned  INT     NOT NULL DEFAULT 0,
    reviews_given        INT     NOT NULL DEFAULT 0,
    total_hours_completed INT    NOT NULL DEFAULT 0,
    updated_at           DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User Achievements / Badges
CREATE TABLE IF NOT EXISTS user_achievements (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    badge_type        VARCHAR(50)   NOT NULL,
    badge_name        VARCHAR(100)  NOT NULL,
    badge_description VARCHAR(300),
    xp_awarded        INT           NOT NULL DEFAULT 0,
    icon_url          VARCHAR(300),
    earned_at         DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_achievement_user  (user_id),
    INDEX idx_achievement_badge (badge_type)
);

-- Networking Connections
CREATE TABLE IF NOT EXISTS networking_connections (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    requester_id  BIGINT        NOT NULL,
    receiver_id   BIGINT        NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    match_reason  VARCHAR(400),
    match_score   DOUBLE,
    created_at    DATETIME,
    updated_at    DATETIME,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id)  REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_net_requester (requester_id),
    INDEX idx_net_receiver  (receiver_id)
);

-- Event Predictions
CREATE TABLE IF NOT EXISTS event_predictions (
    id                        BIGINT         PRIMARY KEY AUTO_INCREMENT,
    event_id                  BIGINT         NOT NULL UNIQUE,
    predicted_registrations   INT,
    predicted_attendance      INT,
    predicted_no_show_rate    DECIMAL(5,2),
    predicted_revenue         DECIMAL(12,2),
    predicted_food_count      INT,
    predicted_certificate_count INT,
    success_score             DECIMAL(5,2),
    ai_suggestions            TEXT,
    budget_estimate           DECIMAL(12,2),
    expected_revenue          DECIMAL(12,2),
    break_even_point          DECIMAL(12,2),
    estimated_profit          DECIMAL(12,2),
    created_at                DATETIME,
    updated_at                DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- College Rankings Leaderboard
CREATE TABLE IF NOT EXISTS college_rankings (
    id                   BIGINT        PRIMARY KEY AUTO_INCREMENT,
    college_name         VARCHAR(200)  NOT NULL UNIQUE,
    total_participants   BIGINT        NOT NULL DEFAULT 0,
    total_events_attended BIGINT       NOT NULL DEFAULT 0,
    total_certificates   BIGINT        NOT NULL DEFAULT 0,
    average_rating       DECIMAL(4,2)  NOT NULL DEFAULT 0,
    overall_score        DECIMAL(8,2)  NOT NULL DEFAULT 0,
    rank_position        INT           NOT NULL DEFAULT 0,
    updated_at           DATETIME,
    INDEX idx_college_name (college_name)
);

-- ═══════════════════════════════════════════════════════════════════
-- CERTIFICATE SYSTEM TABLES
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS certificate_settings (
    id                        BIGINT         PRIMARY KEY AUTO_INCREMENT,
    event_id                  BIGINT         NOT NULL UNIQUE,
    certificate_available     BOOLEAN        NOT NULL DEFAULT FALSE,
    automatic_generation      BOOLEAN        NOT NULL DEFAULT FALSE,
    release_mode              VARCHAR(30)    NOT NULL DEFAULT 'MANUAL',
    release_date              DATE,
    minimum_attendance_required BOOLEAN      NOT NULL DEFAULT TRUE,
    certificate_type          VARCHAR(30)    NOT NULL DEFAULT 'PARTICIPATION',
    organizer_signature_url   VARCHAR(500),
    organizer_name            VARCHAR(160),
    organization_name         VARCHAR(200),
    verification_base_url     VARCHAR(500),
    certificate_expiry        DATE,
    theme                     VARCHAR(40)    DEFAULT 'MODERN_BLUE',
    released                  BOOLEAN        NOT NULL DEFAULT FALSE,
    released_at               DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS certificate_templates (
    id                BIGINT         PRIMARY KEY AUTO_INCREMENT,
    event_id          BIGINT         NOT NULL,
    template_url      VARCHAR(500)   NOT NULL,
    original_file_name VARCHAR(300),
    content_type      VARCHAR(100),
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    uploaded_at       DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_cert_template_event (event_id)
);

CREATE TABLE IF NOT EXISTS certificate_history (
    id               BIGINT         PRIMARY KEY AUTO_INCREMENT,
    certificate_id   VARCHAR(50)    NOT NULL,
    certificate_db_id BIGINT,
    action           VARCHAR(50)    NOT NULL,
    actor_id         BIGINT,
    actor_role       VARCHAR(20),
    note             VARCHAR(500),
    created_at       DATETIME,
    FOREIGN KEY (certificate_db_id) REFERENCES certificates(id) ON DELETE SET NULL,
    INDEX idx_cert_history_cert (certificate_id)
);

-- Extend certificates table with new columns
ALTER TABLE certificates
    ADD COLUMN IF NOT EXISTS image_url              VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS verification_token     VARCHAR(80)  NULL,
    ADD COLUMN IF NOT EXISTS certificate_type       VARCHAR(30)  NULL DEFAULT 'PARTICIPATION',
    ADD COLUMN IF NOT EXISTS participant_id         VARCHAR(80)  NULL,
    ADD COLUMN IF NOT EXISTS participant_email      VARCHAR(160) NULL,
    ADD COLUMN IF NOT EXISTS organizer_name         VARCHAR(160) NULL,
    ADD COLUMN IF NOT EXISTS organization_name      VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS verification_url       VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS release_date           DATETIME     NULL,
    ADD COLUMN IF NOT EXISTS released_at            DATETIME     NULL,
    ADD COLUMN IF NOT EXISTS revoked_at             DATETIME     NULL,
    ADD COLUMN IF NOT EXISTS revoke_reason          VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS download_count         BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS verification_count     BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_attempt_count    INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_failure_reason   VARCHAR(500) NULL;
