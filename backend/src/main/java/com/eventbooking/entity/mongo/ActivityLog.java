package com.eventbooking.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for auditing all user and organizer actions.
 */
@Document(collection = "activity_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityLog {

    @Id
    private String id;

    @Indexed
    private Long actorId;

    private String actorType;           // USER / ORGANIZER / SYSTEM

    @Indexed
    private String action;              // LOGIN, BOOK_TICKET, CREATE_EVENT, etc.

    private String entityType;          // Event / Booking / Payment
    private String entityId;

    private Map<String, Object> metadata;

    private String ipAddress;
    private String userAgent;

    @Indexed
    private LocalDateTime timestamp;

    private boolean success;
    private String errorMessage;
}
