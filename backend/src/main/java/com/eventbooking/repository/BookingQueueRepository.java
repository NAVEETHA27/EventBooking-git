package com.eventbooking.repository;

import com.eventbooking.entity.BookingQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingQueueRepository extends JpaRepository<BookingQueueEntry, String> {
}
