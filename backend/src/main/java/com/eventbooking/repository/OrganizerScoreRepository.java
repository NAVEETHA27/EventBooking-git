package com.eventbooking.repository;

import com.eventbooking.entity.OrganizerScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerScoreRepository extends JpaRepository<OrganizerScore, Long> {

    Optional<OrganizerScore> findByOrganizerId(Long organizerId);

    @Query("SELECT os FROM OrganizerScore os ORDER BY os.overallScore DESC")
    Page<OrganizerScore> findTopOrganizers(Pageable pageable);

    @Query("SELECT os FROM OrganizerScore os WHERE os.featured = true ORDER BY os.overallScore DESC")
    Page<OrganizerScore> findFeaturedOrganizers(Pageable pageable);
}
