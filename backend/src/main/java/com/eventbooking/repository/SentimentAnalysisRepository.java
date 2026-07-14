package com.eventbooking.repository;

import com.eventbooking.entity.SentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long> {
    Optional<SentimentAnalysis> findByEventId(Long eventId);
}
