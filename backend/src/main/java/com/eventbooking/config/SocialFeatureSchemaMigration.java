package com.eventbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class SocialFeatureSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        execute("""
            CREATE TABLE IF NOT EXISTS event_community_messages (
                id BIGINT NOT NULL AUTO_INCREMENT,
                event_id BIGINT NOT NULL,
                sender_id BIGINT NOT NULL,
                sender_role VARCHAR(30) NOT NULL,
                sender_name VARCHAR(160) NOT NULL,
                message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
                message TEXT NOT NULL,
                attachment_url VARCHAR(500) NULL,
                pinned TINYINT(1) NOT NULL DEFAULT 0,
                announcement TINYINT(1) NOT NULL DEFAULT 0,
                edited TINYINT(1) NOT NULL DEFAULT 0,
                deleted TINYINT(1) NOT NULL DEFAULT 0,
                moderation_status VARCHAR(30) NOT NULL DEFAULT 'CLEAN',
                sent_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                edited_at DATETIME NULL,
                deleted_at DATETIME NULL,
                PRIMARY KEY (id),
                INDEX idx_event_community_event (event_id, sent_at),
                INDEX idx_event_community_sent_at (sent_at),
                INDEX idx_event_community_sender (sender_id, sender_role),
                CONSTRAINT fk_event_community_event FOREIGN KEY (event_id) REFERENCES events(id)
            )
            """);
        execute("ALTER TABLE event_community_messages ADD COLUMN message TEXT NULL");
        execute("UPDATE event_community_messages SET message = content WHERE (message IS NULL OR message = '') AND content IS NOT NULL");
        execute("ALTER TABLE event_community_messages MODIFY COLUMN message TEXT NOT NULL");
        execute("ALTER TABLE event_community_messages ADD COLUMN content TEXT NULL");
        execute("UPDATE event_community_messages SET content = message WHERE (content IS NULL OR content = '') AND message IS NOT NULL");
        execute("ALTER TABLE event_community_messages MODIFY COLUMN content TEXT NULL");

        execute("""
            CREATE TABLE IF NOT EXISTS networking_connections (
                id BIGINT NOT NULL AUTO_INCREMENT,
                requester_id BIGINT NOT NULL,
                receiver_id BIGINT NOT NULL,
                status VARCHAR(20) NULL DEFAULT 'PENDING',
                match_reason VARCHAR(400) NULL,
                match_score DOUBLE NULL,
                created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NULL,
                PRIMARY KEY (id),
                INDEX idx_net_requester (requester_id),
                INDEX idx_net_receiver (receiver_id),
                INDEX idx_net_status (status),
                CONSTRAINT fk_net_requester FOREIGN KEY (requester_id) REFERENCES users(id),
                CONSTRAINT fk_net_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
            )
            """);

        execute("""
            CREATE TABLE IF NOT EXISTS direct_messages (
                id BIGINT NOT NULL AUTO_INCREMENT,
                sender_id BIGINT NOT NULL,
                receiver_id BIGINT NOT NULL,
                content TEXT NOT NULL,
                is_read TINYINT(1) NOT NULL DEFAULT 0,
                sent_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                INDEX idx_dm_sender (sender_id),
                INDEX idx_dm_receiver (receiver_id),
                INDEX idx_dm_conversation (sender_id, receiver_id, sent_at),
                CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES users(id),
                CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
            )
            """);

        execute("""
            CREATE TABLE IF NOT EXISTS event_ratings (
                id BIGINT NOT NULL AUTO_INCREMENT,
                event_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                booking_id BIGINT NULL,
                overall_rating INT NOT NULL,
                speaker_rating INT NULL,
                venue_rating INT NULL,
                food_rating INT NULL,
                organization_rating INT NULL,
                time_management_rating INT NULL,
                content_quality_rating INT NULL,
                value_for_money_rating INT NULL,
                review_text TEXT NULL,
                photo_urls TEXT NULL,
                is_verified_attendance TINYINT(1) NOT NULL DEFAULT 0,
                moderation_status VARCHAR(20) NULL DEFAULT 'APPROVED',
                moderation_note VARCHAR(500) NULL,
                is_fake_flagged TINYINT(1) NULL DEFAULT 0,
                sentiment VARCHAR(20) NULL,
                sentiment_score DOUBLE NULL,
                is_testimonial_candidate TINYINT(1) NULL DEFAULT 0,
                created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NULL,
                PRIMARY KEY (id),
                UNIQUE KEY uk_rating_user_event (user_id, event_id),
                INDEX idx_rating_event (event_id),
                INDEX idx_rating_user (user_id),
                INDEX idx_rating_verified (is_verified_attendance),
                CONSTRAINT fk_rating_event FOREIGN KEY (event_id) REFERENCES events(id),
                CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id),
                CONSTRAINT fk_rating_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
            )
            """);
        execute("ALTER TABLE event_ratings ADD COLUMN event_quality_rating INT NULL");
        execute("ALTER TABLE event_ratings ADD COLUMN accommodation_rating INT NULL");
        execute("ALTER TABLE event_ratings ADD COLUMN suggestions TEXT NULL");
        execute("ALTER TABLE event_ratings ADD COLUMN anonymous TINYINT(1) NOT NULL DEFAULT 0");

        log.info("SocialFeatureSchemaMigration completed");
    }

    private void execute(String sql) {
        try {
            jdbc.execute(sql);
        } catch (Exception ex) {
            log.warn("SocialFeatureSchemaMigration skipped statement: {}", ex.getMessage());
        }
    }
}
