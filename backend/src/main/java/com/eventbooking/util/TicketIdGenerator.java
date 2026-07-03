package com.eventbooking.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates human-readable unique ticket IDs.
 * Format: EVT-YYYYMMDD-XXXXXXXX  (e.g. EVT-20240615-A3F9B2C1)
 */
public final class TicketIdGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TicketIdGenerator() {}

    public static String generate() {
        String date    = LocalDateTime.now().format(FMT);
        String segment = ThreadLocalRandom.current()
                .ints(8, 0, CHARS.length())
                .collect(StringBuilder::new,
                        (sb, i) -> sb.append(CHARS.charAt(i)),
                        StringBuilder::append)
                .toString();
        return "EVT-" + date + "-" + segment;
    }
}
