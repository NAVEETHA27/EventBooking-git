package com.eventbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup migration — ensures organizer_code column + unique index exist,
 * and every user/organizer row has a correctly formatted ID code.
 *
 * Compatible with MySQL 5.7+ and MySQL 8.x.
 *
 * Uses INFORMATION_SCHEMA queries instead of "IF NOT EXISTS" DDL clauses
 * because "ADD COLUMN IF NOT EXISTS" is not available on MySQL 5.7.
 *
 * Fully idempotent — safe to run on every application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdCodeMigration {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void run() {
        try {
            ensureOrganizerCodeColumn();
            ensureOrganizerCodeIndex();

            int fixedUsers = fixUsers();
            int fixedOrgs  = fixOrganizers();

            if (fixedUsers > 0 || fixedOrgs > 0) {
                log.info("[IdCodeMigration] Fixed {} user code(s) and {} organizer code(s)",
                        fixedUsers, fixedOrgs);
            } else {
                log.debug("[IdCodeMigration] All ID codes are correctly formatted — nothing to fix");
            }
        } catch (Exception ex) {
            log.warn("[IdCodeMigration] Migration failed: {}", ex.getMessage());
        }
    }

    // ── Column ────────────────────────────────────────────────────────────────

    /**
     * Adds organizer_code VARCHAR(30) if it does not already exist.
     * Checks INFORMATION_SCHEMA.COLUMNS — works on MySQL 5.7 and 8.x.
     */
    private void ensureOrganizerCodeColumn() {
        String dbName = currentDatabase();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'organizers' AND COLUMN_NAME = 'organizer_code'",
                Integer.class, dbName);

        if (count == null || count == 0) {
            jdbc.execute("ALTER TABLE organizers ADD COLUMN organizer_code VARCHAR(30) NULL");
            log.info("[IdCodeMigration] Created organizers.organizer_code column");
        } else {
            log.debug("[IdCodeMigration] organizers.organizer_code column already exists — skipping");
        }
    }

    // ── Index ─────────────────────────────────────────────────────────────────

    /**
     * Adds a UNIQUE index on organizer_code if it does not already exist.
     * Checks INFORMATION_SCHEMA.STATISTICS — works on MySQL 5.7 and 8.x.
     */
    private void ensureOrganizerCodeIndex() {
        String dbName = currentDatabase();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'organizers' AND INDEX_NAME = 'idx_org_public_id'",
                Integer.class, dbName);

        if (count == null || count == 0) {
            jdbc.execute(
                "ALTER TABLE organizers ADD UNIQUE INDEX idx_org_public_id (organizer_code)");
            log.info("[IdCodeMigration] Created UNIQUE index idx_org_public_id on organizers.organizer_code");
        } else {
            log.debug("[IdCodeMigration] idx_org_public_id index already exists — skipping");
        }
    }

    // ── Data fixes ────────────────────────────────────────────────────────────

    /** Fix NULL/empty user_code and old USR-XXXX → USRXXXX format */
    private int fixUsers() {
        int nullFixed = jdbc.update(
                "UPDATE users SET user_code = CONCAT('USR', LPAD(id, 4, '0')) " +
                "WHERE user_code IS NULL OR user_code = ''");
        int fmtFixed = jdbc.update(
                "UPDATE users SET user_code = REPLACE(user_code, 'USR-', 'USR') " +
                "WHERE user_code LIKE 'USR-%'");
        return nullFixed + fmtFixed;
    }

    /** Fix NULL/empty organizer_code and old ORG-XXXX → ORGXXXX format */
    private int fixOrganizers() {
        int nullFixed = jdbc.update(
                "UPDATE organizers SET organizer_code = CONCAT('ORG', LPAD(id, 4, '0')) " +
                "WHERE organizer_code IS NULL OR organizer_code = ''");
        int fmtFixed = jdbc.update(
                "UPDATE organizers SET organizer_code = REPLACE(organizer_code, 'ORG-', 'ORG') " +
                "WHERE organizer_code LIKE 'ORG-%'");
        return nullFixed + fmtFixed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String currentDatabase() {
        String db = jdbc.queryForObject("SELECT DATABASE()", String.class);
        return db != null ? db : "event_booking_db";
    }
}
