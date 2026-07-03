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
    INDEX idx_org_email (email)
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

UPDATE users SET user_code = CONCAT('USR', LPAD(id, 4, '0')) WHERE user_code IS NULL OR user_code = '';
UPDATE organizers SET organizer_code = CONCAT('ORG', LPAD(id, 4, '0')) WHERE organizer_code IS NULL OR organizer_code = '';
