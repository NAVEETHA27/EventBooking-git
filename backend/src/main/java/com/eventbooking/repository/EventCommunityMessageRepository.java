package com.eventbooking.repository;

import com.eventbooking.entity.EventCommunityMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventCommunityMessageRepository extends JpaRepository<EventCommunityMessage, Long> {

    @Query("""
        SELECT m FROM EventCommunityMessage m
        WHERE m.event.id = :eventId
        ORDER BY m.sentAt DESC, m.id DESC
    """)
    List<EventCommunityMessage> findLatestByEvent(@Param("eventId") Long eventId, Pageable pageable);

    @Query("""
        SELECT m FROM EventCommunityMessage m
        WHERE m.event.id = :eventId AND m.id > :afterId
        ORDER BY m.sentAt ASC, m.id ASC
    """)
    List<EventCommunityMessage> findNewerThan(@Param("eventId") Long eventId, @Param("afterId") Long afterId, Pageable pageable);

    @Query("""
        SELECT m FROM EventCommunityMessage m
        WHERE m.event.id = :eventId AND m.id < :beforeId
        ORDER BY m.sentAt DESC, m.id DESC
    """)
    List<EventCommunityMessage> findOlderThan(@Param("eventId") Long eventId, @Param("beforeId") Long beforeId, Pageable pageable);

    List<EventCommunityMessage> findByEventIdAndPinnedTrueAndDeletedFalseOrderBySentAtDesc(Long eventId);
}
