package com.eventbooking.repository;

import com.eventbooking.entity.EventPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventPredictionRepository extends JpaRepository<EventPrediction, Long> {
    Optional<EventPrediction> findByEventId(Long eventId);
}
