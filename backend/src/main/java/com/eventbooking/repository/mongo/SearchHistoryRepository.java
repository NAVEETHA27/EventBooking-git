package com.eventbooking.repository.mongo;

import com.eventbooking.entity.mongo.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchHistoryRepository extends MongoRepository<SearchHistory, String> {
    Page<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);
}
