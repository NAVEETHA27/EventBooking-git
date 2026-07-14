package com.eventbooking.repository;

import com.eventbooking.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEventIdAndEmailIgnoreCase(Long eventId, String email);
    List<Participant> findByEventIdAndEmailIn(Long eventId, Collection<String> emails);
    List<Participant> findByBookingId(Long bookingId);

    /** Eagerly fetch booking and event in one query — avoids LazyInitializationException. */
    @Query("""
        SELECT p FROM Participant p
        JOIN FETCH p.booking b
        JOIN FETCH p.event e
        WHERE e.organizer.id = :organizerId
    """)
    List<Participant> findByEventOrganizerIdWithDetails(@Param("organizerId") Long organizerId);
}
