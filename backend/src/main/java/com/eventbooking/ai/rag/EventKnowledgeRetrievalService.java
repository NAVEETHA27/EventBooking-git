package com.eventbooking.ai.rag;

import com.eventbooking.entity.Event;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.FaqRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventKnowledgeRetrievalService {
    private static final int MAX_DOCUMENTS = 12;

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<RagDocument> retrieve(String query, AuthPrincipal principal) {
        Set<String> terms = tokenize(query);
        List<RagDocument> documents = new ArrayList<>();

        documents.add(new RagDocument(
                "POLICY",
                "public-event-lifecycle",
                "Core platform policies",
                "Discover shows published upcoming public events. Confirmed bookings create QR tickets for venue entry. " +
                        "Successful paid bookings can create refunds when cancelled. Certificates are available after eligible completed events.",
                score("booking payment refund certificate qr cancellation registration schedule", terms)));

        eventRepository.findUpcomingPublicEvents(LocalDate.now()).stream()
                .limit(30)
                .map(event -> eventDocument(event, terms))
                .forEach(documents::add);

        faqRepository.findByActiveTrueOrderByCategoryAscQuestionAsc().stream()
                .limit(30)
                .map(faq -> new RagDocument(
                        "FAQ",
                        String.valueOf(faq.getId()),
                        faq.getCategory() + ": " + faq.getQuestion(),
                        faq.getAnswer(),
                        score(faq.getCategory() + " " + faq.getQuestion() + " " + faq.getAnswer(), terms)))
                .forEach(documents::add);

        if (principal != null && "USER".equalsIgnoreCase(principal.getRole())) {
            bookingRepository.findByUserId(principal.getId(), PageRequest.of(0, 10, Sort.by("bookedAt").descending()))
                    .getContent()
                    .forEach(booking -> documents.add(new RagDocument(
                            "USER_BOOKING",
                            String.valueOf(booking.getId()),
                            "Booking " + booking.getTicketId(),
                            "Event: " + booking.getEvent().getEventName() +
                                    ". Date: " + booking.getEvent().getEventDate() +
                                    ". Booking status: " + booking.getBookingStatus() +
                                    ". Ticket status: " + booking.getTicketStatus() +
                                    ". Quantity: " + booking.getQuantity() + ".",
                            score(booking.getEvent().getEventName() + " booking ticket qr", terms) + 4)));

            paymentRepository.findByBookingUserId(principal.getId(), PageRequest.of(0, 5, Sort.by("paidAt").descending()))
                    .forEach(payment -> documents.add(new RagDocument(
                            "USER_PAYMENT",
                            String.valueOf(payment.getId()),
                            "Payment " + payment.getTransactionId(),
                            "Payment status: " + payment.getPaymentStatus() + ". Amount: " + payment.getAmount() + ".",
                            score("payment paid transaction amount", terms) + 3)));

            refundRepository.findByUserId(principal.getId()).stream()
                    .limit(5)
                    .forEach(refund -> documents.add(new RagDocument(
                            "USER_REFUND",
                            String.valueOf(refund.getId()),
                            "Refund " + refund.getRefundReference(),
                            "Refund status: " + refund.getRefundStatus() + ". Amount: " + refund.getRefundAmount() + ".",
                            score("refund cancellation money", terms) + 3)));
        }

        if (principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole())) {
            eventRepository.findByOrganizerId(principal.getId(), PageRequest.of(0, 10, Sort.by("createdAt").descending()))
                    .getContent()
                    .forEach(event -> documents.add(new RagDocument(
                            "ORGANIZER_EVENT",
                            String.valueOf(event.getId()),
                            "Organizer event " + event.getEventName(),
                            "Status: " + event.getStatus() +
                                    ". Date: " + event.getEventDate() +
                                    ". Seats: " + event.getAvailableSeats() + "/" + event.getTotalSeats() +
                                    ". Category: " + event.getCategory() + ".",
                            score(event.getEventName() + " organizer attendance registration revenue", terms) + 4)));
        }

        return documents.stream()
                .sorted(Comparator.comparingInt(RagDocument::score).reversed())
                .limit(MAX_DOCUMENTS)
                .toList();
    }

    public String formatContext(List<RagDocument> documents) {
        StringBuilder context = new StringBuilder();
        for (RagDocument document : documents) {
            context.append("- [").append(document.sourceType()).append(":").append(document.sourceId()).append("] ")
                    .append(document.title()).append(" - ").append(document.content()).append("\n");
        }
        return context.toString();
    }

    private RagDocument eventDocument(Event event, Set<String> terms) {
        String content = "Event: " + event.getEventName() +
                ". Category: " + nullSafe(event.getCategory()) +
                ". College: " + nullSafe(event.getCollegeName()) +
                ". Department: " + nullSafe(event.getDepartmentName()) +
                ". Venue: " + nullSafe(event.getVenueName()) +
                ". Location: " + nullSafe(event.getLocation()) +
                ". Date: " + event.getEventDate() +
                ". Seats: " + event.getAvailableSeats() +
                ". Price: " + event.getTicketPrice() +
                ". Type: " + nullSafe(event.getEventType()) + ".";
        return new RagDocument("EVENT", String.valueOf(event.getId()), event.getEventName(), content,
                score(content + " " + nullSafe(event.getTags()), terms));
    }

    private int score(String content, Set<String> terms) {
        if (terms.isEmpty() || !StringUtils.hasText(content)) return 1;
        String haystack = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) score++;
        }
        return score;
    }

    private Set<String> tokenize(String query) {
        Set<String> terms = new LinkedHashSet<>();
        if (!StringUtils.hasText(query)) {
            return terms;
        }
        for (String token : query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= 3) {
                terms.add(token);
            }
        }
        return terms;
    }

    private String nullSafe(String value) {
        return value != null ? value : "N/A";
    }
}
