package com.eventbooking.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for in-app and email notifications.
 */
@Document(collection = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long recipientId;           // userId or organizerId

    private String recipientType;       // USER / ORGANIZER

    @Indexed
    private String notificationType;    // BOOKING_CONFIRMED, PAYMENT_SUCCESS, etc.

    private String title;
    private String message;

    @Indexed
    private boolean read;

    private String actionUrl;           // deep-link target

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
