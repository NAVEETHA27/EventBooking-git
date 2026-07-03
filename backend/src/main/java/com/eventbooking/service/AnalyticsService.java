package com.eventbooking.service;

import com.eventbooking.entity.mongo.AnalyticsData;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.mongo.AnalyticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AnalyticsService {

    private final Optional<AnalyticsRepository> analyticsRepository;
    private final BookingRepository   bookingRepository;
    private final PaymentRepository   paymentRepository;
    private final EventRepository     eventRepository;

    @Autowired
    public AnalyticsService(
            @Autowired(required = false) AnalyticsRepository analyticsRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            EventRepository eventRepository) {
        this.analyticsRepository = Optional.ofNullable(analyticsRepository);
        this.bookingRepository   = bookingRepository;
        this.paymentRepository   = paymentRepository;
        this.eventRepository     = eventRepository;
    }

    public Map<String, Object> getOrganizerDashboard(Long organizerId) {
        Map<String, Object> dashboard = new HashMap<>();

        // Total events
        long totalEvents = eventRepository.findByOrganizerId(organizerId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        dashboard.put("totalEvents", totalEvents);

        // Total revenue
        BigDecimal revenue = paymentRepository.sumRevenueByOrganizer(organizerId);
        dashboard.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);

        // Analytics data last 30 days
        LocalDate from = LocalDate.now().minusDays(30);
        List<AnalyticsData> analyticsData = analyticsRepository
                .map(repo -> repo.findByOrganizerIdAndDateBetween(organizerId, from, LocalDate.now()))
                .orElseGet(List::of);
        dashboard.put("analyticsLast30Days", analyticsData);

        return dashboard;
    }

    public Map<String, Object> getUserDashboard(Long userId) {
        Map<String, Object> dashboard = new HashMap<>();
        long totalBookings = bookingRepository.findByUserId(userId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        dashboard.put("totalBookings", totalBookings);
        return dashboard;
    }

    public void recordDailySnapshot(Long organizerId, Long eventId,
                                     int bookings, int tickets, BigDecimal revenue, int cancellations) {
        if (analyticsRepository.isEmpty()) {
            log.warn("MongoDB is disabled or unavailable; skipping analytics snapshot for event {}", eventId);
            return;
        }
        LocalDate today = LocalDate.now();
        AnalyticsRepository repo = analyticsRepository.get();
        AnalyticsData existing = repo.findByEventIdAndDate(eventId, today).orElse(null);
        if (existing != null) {
            existing.setTotalBookings(existing.getTotalBookings() + bookings);
            existing.setTotalTicketsSold(existing.getTotalTicketsSold() + tickets);
            existing.setTotalRevenue(existing.getTotalRevenue().add(revenue));
            existing.setTotalCancellations(existing.getTotalCancellations() + cancellations);
            existing.setUpdatedAt(LocalDateTime.now());
            repo.save(existing);
        } else {
            repo.save(AnalyticsData.builder()
                    .organizerId(organizerId)
                    .eventId(eventId)
                    .date(today)
                    .totalBookings(bookings)
                    .totalTicketsSold(tickets)
                    .totalRevenue(revenue)
                    .totalCancellations(cancellations)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
    }
}
