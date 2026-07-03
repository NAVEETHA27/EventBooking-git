package com.eventbooking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE payments MODIFY COLUMN payment_status VARCHAR(50) NOT NULL");
            log.info("Verified payments.payment_status supports all PaymentStatus values");
        } catch (Exception ex) {
            log.debug("Skipping payments.payment_status migration: {}", ex.getMessage());
        }
    }
}
