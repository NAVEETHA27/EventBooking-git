package com.eventbooking.entity.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for tracking user search history.
 */
@Document(collection = "search_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SearchHistory {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String keyword;
    private String category;
    private String eventType;
    private String location;
    private String dateRange;
    private int resultCount;

    @Indexed
    private LocalDateTime searchedAt;
}
