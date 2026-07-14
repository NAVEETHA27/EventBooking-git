package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.entity.Booking;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.function.Function;

@Component("bookingTool")
@org.springframework.context.annotation.Description("View user bookings, tickets, and reservations status")
@RequiredArgsConstructor
public class BookingTool implements AgentTool, Function<Map<String, Object>, Map<String, Object>> {

    private final BookingRepository bookingRepository;

    @Override
    public String name() { return "bookingTool"; }

    @Override
    public String description() {
        return "Retrieves user bookings, booking status, ticket info, and QR code availability.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (principal == null) {
            result.put("error", "Authentication required to view bookings.");
            return result;
        }
        try {
            Long userId = principal.getId();
            List<Booking> bookings = bookingRepository
                    .findByUserId(userId, PageRequest.of(0, 10, Sort.by("bookedAt").descending()))
                    .getContent();

            List<Map<String, Object>> bookingList = bookings.stream().map(b -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bookingId", b.getId());
                m.put("ticketId", b.getTicketId());
                m.put("eventName", b.getEvent() != null ? b.getEvent().getEventName() : "N/A");
                m.put("eventDate", b.getEvent() != null ? b.getEvent().getEventDate() : null);
                m.put("bookingStatus", b.getBookingStatus());
                m.put("ticketStatus", b.getTicketStatus());
                m.put("quantity", b.getQuantity());
                m.put("totalAmount", b.getTotalAmount());
                m.put("hasQr", b.getQrCodePath() != null);
                m.put("bookedAt", b.getBookedAt());
                return m;
            }).collect(Collectors.toList());

            result.put("bookings", bookingList);
            result.put("count", bookingList.size());
            long confirmed = bookings.stream()
                    .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CONFIRMED).count();
            result.put("confirmedCount", confirmed);
        } catch (Exception ex) {
            result.put("error", "Could not fetch bookings: " + ex.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {
        return execute(input, null);
    }
}
