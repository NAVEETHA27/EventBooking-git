package com.eventbooking.repository;

import com.eventbooking.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    /** Full conversation between two users, oldest first */
    @Query("""
        SELECT m FROM DirectMessage m
        WHERE (m.sender.id = :a AND m.receiver.id = :b)
           OR (m.sender.id = :b AND m.receiver.id = :a)
        ORDER BY m.sentAt ASC
    """)
    List<DirectMessage> findConversation(@Param("a") Long a, @Param("b") Long b);

    /** Poll for new messages since lastId */
    @Query("""
        SELECT m FROM DirectMessage m
        WHERE ((m.sender.id = :a AND m.receiver.id = :b)
            OR (m.sender.id = :b AND m.receiver.id = :a))
          AND m.id > :lastId
        ORDER BY m.sentAt ASC
    """)
    List<DirectMessage> pollNew(@Param("a") Long a, @Param("b") Long b, @Param("lastId") Long lastId);

    /** Unread count for a user */
    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE m.receiver.id = :userId AND m.read = false")
    long countUnread(@Param("userId") Long userId);

    /** Mark conversation as read */
    @Modifying
    @Transactional
    @Query("UPDATE DirectMessage m SET m.read = true WHERE m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.read = false")
    void markRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
