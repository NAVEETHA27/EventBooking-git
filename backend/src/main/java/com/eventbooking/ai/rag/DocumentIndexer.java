package com.eventbooking.ai.rag;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.EventRating;
import com.eventbooking.entity.SentimentAnalysis;
import com.eventbooking.repository.CertificateRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.EventRatingRepository;
import com.eventbooking.repository.FaqRepository;
import com.eventbooking.repository.SentimentAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DocumentIndexer {
    private final EventRepository eventRepository;
    private final FaqRepository faqRepository;
    private final EventRatingRepository eventRatingRepository;
    private final CertificateRepository certificateRepository;
    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    private final DocumentChunker documentChunker;
    private final VectorIndexService vectorIndexService;

    @Transactional
    public int indexCoreKnowledge() {
        int indexed = 0;
        indexed += indexPolicies();
        indexed += indexFaqs();
        indexed += indexEvents();
        indexed += indexReviews();
        indexed += indexFeedbackSummaries();
        indexed += indexCertificateTemplates();
        return indexed;
    }

    @Transactional
    public int indexEvent(Event event) {
        String content = """
                Event: %s. Category: %s. Type: %s. College: %s. Department: %s.
                Venue: %s. Location: %s. Date: %s. Price: %s. Seats available: %s.
                Description: %s. Organizer details: %s. Travel guide: %s.
                Food: %s. Accommodation: %s. Authorized document: %s.
                """.formatted(
                safe(event.getEventName()), safe(event.getCategory()), safe(event.getEventType()),
                safe(event.getCollegeName()), safe(event.getDepartmentName()), safe(event.getVenueName()),
                safe(event.getLocation()), event.getEventDate(), event.getTicketPrice(), event.getAvailableSeats(),
                safe(event.getDescription()), safe(event.getOrganizerDetails()), safe(event.getTravelGuide()),
                Boolean.TRUE.equals(event.getFoodProvided()) ? safe(event.getFoodMeals()) : "Not provided",
                Boolean.TRUE.equals(event.getAccommodationProvided()) ? safe(event.getAccommodationDetails()) : "Not provided",
                safe(event.getAuthorizedDocumentUrl()));
        return vectorIndexService.index("EVENT", String.valueOf(event.getId()), event.getEventName(), documentChunker.chunk(content));
    }

    private int indexEvents() {
        return eventRepository.findUpcomingPublicEvents(LocalDate.now()).stream()
                .mapToInt(this::indexEvent)
                .sum();
    }

    private int indexFaqs() {
        return faqRepository.findByActiveTrueOrderByCategoryAscQuestionAsc().stream()
                .mapToInt(faq -> vectorIndexService.index(
                        "FAQ",
                        String.valueOf(faq.getId()),
                        faq.getQuestion(),
                        documentChunker.chunk(faq.getCategory() + ". " + faq.getQuestion() + " " + faq.getAnswer())))
                .sum();
    }

    private int indexReviews() {
        return eventRatingRepository.findAll().stream()
                .filter(review -> review.getModerationStatus() == EventRating.ModerationStatus.APPROVED)
                .filter(review -> review.getReviewText() != null && !review.getReviewText().isBlank())
                .mapToInt(review -> vectorIndexService.index(
                        "REVIEW",
                        String.valueOf(review.getId()),
                        "Review for " + review.getEvent().getEventName(),
                        documentChunker.chunk("Event: " + review.getEvent().getEventName() +
                                ". Rating: " + review.getOverallRating() +
                                ". Sentiment: " + safe(review.getSentiment()) +
                                ". Review: " + review.getReviewText())))
                .sum();
    }

    private int indexFeedbackSummaries() {
        return sentimentAnalysisRepository.findAll().stream()
                .mapToInt(summary -> vectorIndexService.index(
                        "FEEDBACK_SUMMARY",
                        String.valueOf(summary.getId()),
                        "Feedback summary for " + summary.getEvent().getEventName(),
                        documentChunker.chunk("Event: " + summary.getEvent().getEventName() +
                                ". Positive: " + summary.getPositiveCount() +
                                ". Neutral: " + summary.getNeutralCount() +
                                ". Negative: " + summary.getNegativeCount() +
                                ". Strengths: " + safe(summary.getStrengths()) +
                                ". Weaknesses: " + safe(summary.getWeaknesses()) +
                                ". Improvements: " + safe(summary.getImprovementSuggestions()) +
                                ". Report: " + safe(summary.getImprovementReport()))))
                .sum();
    }

    private int indexCertificateTemplates() {
        String templatePolicy = "Certificate templates populate participant name, college, department, certificate ID, QR code, date, event name, and signature. " +
                "Certificates are issued after event completion for eligible attendees with verified attendance and can be delivered by email.";
        int indexed = vectorIndexService.index("CERTIFICATE_TEMPLATE", "default", "Default certificate template policy", documentChunker.chunk(templatePolicy));
        indexed += certificateRepository.findAll().stream()
                .limit(100)
                .mapToInt(certificate -> vectorIndexService.index(
                        "CERTIFICATE",
                        certificate.getCertificateId(),
                        certificate.getEventName(),
                        documentChunker.chunk("Certificate: " + certificate.getCertificateId() +
                                ". Event: " + certificate.getEventName() +
                                ". Status: " + certificate.getStatus() +
                                ". Issued at: " + certificate.getIssuedAt())))
                .sum();
        return indexed;
    }

    private int indexPolicies() {
        String policies = "Bookings create QR tickets. Refunds apply to successful paid bookings when cancelled. " +
                "Certificates are issued for eligible completed events. Guests can view public event knowledge only. " +
                "Students can access their own bookings, certificates, payments, ratings, recommendations, and portfolio data. " +
                "Organizers can access their own events, participants, analytics, travel data, certificates, and feedback.";
        return vectorIndexService.index("POLICY", "core", "Core EventGPT policies", documentChunker.chunk(policies));
    }

    private String safe(String value) {
        return value != null ? value : "N/A";
    }
}
