package com.eventbooking.repository;

import com.eventbooking.entity.TutorialVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorialVideoRepository extends JpaRepository<TutorialVideo, Long> {
    List<TutorialVideo> findByActiveTrueOrderByCreatedAtDesc();
}
