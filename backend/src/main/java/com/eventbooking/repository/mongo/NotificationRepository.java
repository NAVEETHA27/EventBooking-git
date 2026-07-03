package com.eventbooking.repository.mongo;

import com.eventbooking.entity.mongo.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(
            Long recipientId, String recipientType, Pageable pageable);

    long countByRecipientIdAndRecipientTypeAndReadFalse(Long recipientId, String recipientType);

    void deleteByRecipientIdAndRecipientType(Long recipientId, String recipientType);
}
