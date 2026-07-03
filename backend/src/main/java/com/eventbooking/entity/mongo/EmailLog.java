package com.eventbooking.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "email_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailLog {
    @Id
    private String id;
    private String to;
    private String subject;
    private String status;
    private String errorMessage;
    private Long eventId;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private int attempts;
}
