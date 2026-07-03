package com.eventbooking.repository.mongo;

import com.eventbooking.entity.mongo.AnalyticsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticsRepository extends MongoRepository<AnalyticsData, String> {

    List<AnalyticsData> findByOrganizerIdAndDateBetween(Long organizerId, LocalDate from, LocalDate to);

    Optional<AnalyticsData> findByEventIdAndDate(Long eventId, LocalDate date);

    Page<AnalyticsData> findByOrganizerIdOrderByDateDesc(Long organizerId, Pageable pageable);
}
