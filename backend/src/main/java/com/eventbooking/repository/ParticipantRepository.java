package com.eventbooking.repository;

import com.eventbooking.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEventIdAndEmailIgnoreCase(Long eventId, String email);
    List<Participant> findByEventIdAndEmailIn(Long eventId, Collection<String> emails);
    List<Participant> findByBookingId(Long bookingId);
    List<Participant> findByEventOrganizerId(Long organizerId);
}
