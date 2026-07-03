package com.eventbooking.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB document for daily analytics snapshots.
 */
@Document(collection = "analytics_data")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsData {

    @Id
    private String id;

    @Indexed
    private Long organizerId;

    @Indexed
    private Long eventId;

    @Indexed
    private LocalDate date;

    private int totalBookings;
    private int totalTicketsSold;
    private BigDecimal totalRevenue;
    private int totalCancellations;
    private int uniqueVisitors;
    private int pageViews;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
