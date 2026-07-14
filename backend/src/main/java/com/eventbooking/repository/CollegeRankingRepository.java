package com.eventbooking.repository;

import com.eventbooking.entity.CollegeRanking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollegeRankingRepository extends JpaRepository<CollegeRanking, Long> {
    Optional<CollegeRanking> findByCollegeName(String collegeName);
    Page<CollegeRanking> findAllByOrderByRankPositionAsc(Pageable pageable);
}
