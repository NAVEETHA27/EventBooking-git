package com.eventbooking.repository.mongo;

import com.eventbooking.entity.mongo.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLog, String> {
}
