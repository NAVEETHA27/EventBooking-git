package com.eventbooking.repository.mongo;

import com.eventbooking.entity.mongo.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    Page<ActivityLog> findByActorIdOrderByTimestampDesc(Long actorId, Pageable pageable);
}
