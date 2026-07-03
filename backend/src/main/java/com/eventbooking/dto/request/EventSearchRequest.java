package com.eventbooking.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventSearchRequest {
    private String keyword;
    private String category;
    private String eventType;
    private String location;
    private String collegeName;
    private String departmentName;
    private String organizerName;
    private String venueName;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private Boolean freeOnly;
    private Boolean paidOnly;
    private Boolean interCollege;
    private Boolean intraCollege;
    private Boolean past;
    private String status;
    /** latest | popular | price_asc | price_desc | date_asc | date_desc */
    private String sortBy;
    private int page;
    @Builder.Default
    private int size = 12;
}
