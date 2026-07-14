package com.eventbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup migration: backfills NULL values introduced by ddl-auto:update for newly
 * added boolean columns (food_provided, accommodation_provided) and then alters them
 * to NOT NULL DEFAULT FALSE so existing data is preserved without dropping the table.
 *
 * Safe to run repeatedly — each step is wrapped in its own try/catch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)   // run before MockDataSeeder (CommandLineRunner ordering)
public class EventSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {

        // ── food_provided ─────────────────────────────────────────────────
        backfillAndAlter("food_provided");

        // ── accommodation_provided ────────────────────────────────────────
        backfillAndAlter("accommodation_provided");
        backfillAndAlter("tea_coffee_provided");
        backfillAndAlter("boys_hostel_available");
        backfillAndAlter("girls_hostel_available");
        backfillAndAlter("hotel_tieup_available");

        log.info("EventSchemaMigration completed — food/accommodation columns are NOT NULL DEFAULT FALSE.");
    }

    /**
     * 1. UPDATE NULL rows → 0 (false)
     * 2. MODIFY COLUMN to TINYINT(1) NOT NULL DEFAULT 0
     *
     * Both steps are idempotent: the UPDATE has no effect if no NULLs exist,
     * and the MODIFY is harmless if the column is already NOT NULL.
     */
    private void backfillAndAlter(String column) {
        // Step 1 — fill NULLs
        try {
            int updated = jdbc.update(
                "UPDATE events SET " + column + " = 0 WHERE " + column + " IS NULL"
            );
            if (updated > 0) {
                log.info("EventSchemaMigration: backfilled {} NULL rows in events.{}", updated, column);
            }
        } catch (Exception ex) {
            log.warn("EventSchemaMigration: could not backfill events.{}: {}", column, ex.getMessage());
        }

        // Step 2 — enforce NOT NULL DEFAULT FALSE
        try {
            jdbc.execute(
                "ALTER TABLE events MODIFY COLUMN " + column + " TINYINT(1) NOT NULL DEFAULT 0"
            );
            log.debug("EventSchemaMigration: altered events.{} to NOT NULL DEFAULT 0", column);
        } catch (Exception ex) {
            // Column may already be NOT NULL — that's fine
            log.debug("EventSchemaMigration: skipping ALTER for events.{}: {}", column, ex.getMessage());
        }
    }
}
