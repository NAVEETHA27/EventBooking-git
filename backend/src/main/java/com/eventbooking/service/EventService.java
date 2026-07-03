package com.eventbooking.service;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.request.EventSearchRequest;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.PagedResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Organizer;
import com.eventbooking.entity.ApprovalRequest;
import com.eventbooking.entity.mongo.SearchHistory;
import com.eventbooking.mapper.EventMapper;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ApprovalRequestRepository;
import com.eventbooking.repository.OrganizerRepository;
import com.eventbooking.repository.mongo.SearchHistoryRepository;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventService {

    private final EventRepository           eventRepository;
    private final OrganizerRepository       organizerRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final NotificationService       notificationService;
    private final EmailService              emailService;
    private final AuditService              auditService;
    private final EventMapper               eventMapper;

    /** Optional — null when MongoDB is disabled/unavailable */
    private final Optional<SearchHistoryRepository> searchHistoryRepository;

    @Autowired
    public EventService(
            EventRepository eventRepository,
            OrganizerRepository organizerRepository,
            ApprovalRequestRepository approvalRequestRepository,
            NotificationService notificationService,
            EmailService emailService,
            AuditService auditService,
            EventMapper eventMapper,
            @Autowired(required = false) SearchHistoryRepository searchHistoryRepository) {
        this.eventRepository           = eventRepository;
        this.organizerRepository       = organizerRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.notificationService       = notificationService;
        this.emailService              = emailService;
        this.auditService              = auditService;
        this.eventMapper               = eventMapper;
        this.searchHistoryRepository   = Optional.ofNullable(searchHistoryRepository);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────

    @Transactional
    public EventResponse createEvent(Long organizerId, EventRequest req) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));

        Event event = Event.builder()
                .organizer(organizer)
                .eventName(req.getEventName())
                .description(req.getDescription())
                .category(req.getCategory())
                .eventType(req.getEventType())
                .collegeName(req.getCollegeName())
                .departmentName(req.getDepartmentName())
                .eventDate(req.getEventDate())
                .eventTime(req.getEventTime())
                .endDate(req.getEndDate())
                .endTime(req.getEndTime())
                .registrationDeadline(req.getRegistrationDeadline())
                .venueName(req.getVenueName())
                .location(req.getLocation())
                .googleMapsUrl(req.getGoogleMapsUrl())
                .ticketPrice(req.getTicketPrice())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .status(Event.EventStatus.PENDING_APPROVAL)
                .visibility(req.getVisibility())
                .tags(req.getTags())
                .organizerDetails(req.getOrganizerDetails())
                .authorizedDocumentUrl(req.getAuthorizedDocumentUrl())
                .hasCertificate(req.isHasCertificate())
                .build();

        event = eventRepository.save(event);
        approvalRequestRepository.save(ApprovalRequest.builder()
                .event(event)
                .organizer(organizer)
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .build());
        auditService.record(organizerId, "ORGANIZER", "EVENT_CREATED", "EVENT",
                String.valueOf(event.getId()), "Event submitted for approval");
        log.info("Event created: {} by organizer {}", event.getId(), organizerId);
        return toResponse(event);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────

    @Transactional
    public EventResponse updateEvent(Long eventId, Long organizerId, EventRequest req) {
        Event event = getOwnedEvent(eventId, organizerId);
        event.setEventName(req.getEventName());
        event.setDescription(req.getDescription());
        event.setCategory(req.getCategory());
        event.setEventType(req.getEventType());
        event.setCollegeName(req.getCollegeName());
        event.setDepartmentName(req.getDepartmentName());
        event.setEventDate(req.getEventDate());
        event.setEventTime(req.getEventTime());
        event.setEndDate(req.getEndDate());
        event.setEndTime(req.getEndTime());
        event.setRegistrationDeadline(req.getRegistrationDeadline());
        event.setVenueName(req.getVenueName());
        event.setLocation(req.getLocation());
        event.setGoogleMapsUrl(req.getGoogleMapsUrl());
        event.setTicketPrice(req.getTicketPrice());
        event.setTags(req.getTags());
        event.setOrganizerDetails(req.getOrganizerDetails());
        if (StringUtils.hasText(req.getAuthorizedDocumentUrl())) {
            event.setAuthorizedDocumentUrl(req.getAuthorizedDocumentUrl());
        }
        event.setHasCertificate(req.isHasCertificate());
        event.setStatus(Event.EventStatus.PENDING_APPROVAL);
        event.setVisibility(req.getVisibility());
        approvalRequestRepository.save(ApprovalRequest.builder()
                .event(event)
                .organizer(event.getOrganizer())
                .status(ApprovalRequest.ApprovalStatus.PENDING)
                .build());
        notificationService.notifyEventUpdate(event);
        auditService.record(organizerId, "ORGANIZER", "EVENT_EDITED", "EVENT",
                String.valueOf(event.getId()), "Event edited and resubmitted");
        return toResponse(eventRepository.save(event));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────

    @Transactional
    public void deleteEvent(Long eventId, Long organizerId) {
        Event event = getOwnedEvent(eventId, organizerId);
        event.setStatus(Event.EventStatus.CANCELLED);
        eventRepository.save(event);
        log.info("Event deleted: {}", eventId);
    }

    // ─── CANCEL ───────────────────────────────────────────────────────────

    @Transactional
    public EventResponse cancelEvent(Long eventId, Long organizerId) {
        Event event = getOwnedEvent(eventId, organizerId);
        event.setStatus(Event.EventStatus.CANCELLED);
        event = eventRepository.save(event);
        notificationService.notifyEventCancellation(event);
        auditService.record(organizerId, "ORGANIZER", "EVENT_CANCELLED", "EVENT",
                String.valueOf(event.getId()), "Event cancelled by organizer");
        return toResponse(event);
    }

    // ─── PUBLISH ──────────────────────────────────────────────────────────

    @Transactional
    public EventResponse publishEvent(Long eventId, Long organizerId) {
        Event event = getOwnedEvent(eventId, organizerId);
        if (event.getStatus() != Event.EventStatus.APPROVED && event.getStatus() != Event.EventStatus.PUBLISHED) {
            throw new UnauthorizedException("Admin approval is required before publishing");
        }
        event.setStatus(Event.EventStatus.PUBLISHED);
        auditService.record(organizerId, "ORGANIZER", "EVENT_PUBLISHED", "EVENT",
                String.valueOf(event.getId()), "Approved event published");
        Event saved = eventRepository.save(event);
        emailService.sendNewEventNotification(saved);
        return toResponse(saved);
    }

    @Transactional
    public EventResponse reviewEvent(Long eventId, Long adminId, String decision, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        ApprovalRequest approval = approvalRequestRepository.findFirstByEventIdOrderByRequestedAtDesc(eventId)
                .orElseGet(() -> ApprovalRequest.builder().event(event).organizer(event.getOrganizer()).build());
        switch (decision.toUpperCase()) {
            case "APPROVE" -> {
                event.setStatus(Event.EventStatus.APPROVED);
                approval.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
                notificationService.sendNotification(event.getOrganizer().getId(), "ORGANIZER", "EVENT_APPROVED",
                        "Event approved", event.getEventName() + " has been approved. You can publish it now.", "/organizer/events");
            }
            case "REJECT" -> {
                event.setStatus(Event.EventStatus.REJECTED);
                approval.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
                approval.setReviewNote(reason);
                notificationService.sendNotification(event.getOrganizer().getId(), "ORGANIZER", "EVENT_REJECTED",
                        "Event rejected", reason != null ? reason : "Your event was rejected.", "/organizer/events");
            }
            case "REQUEST_MODIFICATIONS" -> {
                event.setStatus(Event.EventStatus.REJECTED);
                approval.setStatus(ApprovalRequest.ApprovalStatus.MODIFICATION_REQUESTED);
                approval.setReviewNote(reason);
                notificationService.sendNotification(event.getOrganizer().getId(), "ORGANIZER", "EVENT_MODIFICATION_REQUESTED",
                        "Changes requested", reason != null ? reason : "Admin requested modifications.", "/organizer/events");
            }
            default -> throw new IllegalArgumentException("Unsupported approval decision");
        }
        approval.setReviewedBy(adminId);
        approvalRequestRepository.save(approval);
        auditService.record(adminId, "ADMIN", "ADMIN_APPROVAL_" + decision.toUpperCase(), "EVENT",
                String.valueOf(eventId), reason);
        return toResponse(eventRepository.save(event));
    }

    // ─── GET ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getVisibility() != Event.EventVisibility.PUBLIC
                || event.getStatus() == Event.EventStatus.DRAFT) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }

        return toResponse(event);
    }

    // ─── SEARCH ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<EventResponse> search(EventSearchRequest req, Long userId) {
        Specification<Event> spec = buildSpec(req);
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), buildSort(req.getSortBy()));
        Page<Event> page = eventRepository.findAll(spec, pageable);

        if (userId != null && StringUtils.hasText(req.getKeyword()))
            saveSearchHistory(userId, req, (int) page.getTotalElements());

        return toPagedResponse(page);
    }

    // ─── ORGANIZER EVENTS ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<EventResponse> getOrganizerEvents(Long organizerId, String status, int page, int size) {
        Event.EventStatus st = status != null ? Event.EventStatus.valueOf(status) : null;
        Page<Event> result = eventRepository.findByOrganizerIdAndStatus(organizerId, st,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPagedResponse(result);
    }

    // ─── FEATURED / UPCOMING ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventResponse> getFeaturedEvents() {
        return eventRepository.findUpcomingPublicEvents(LocalDate.now())
                .stream().limit(6).map(this::toResponse).toList();
    }

    // ─── AUTO-COMPLETE PAST EVENTS (called by EventScheduler) ────────────

    @Transactional
    public void autoCompletePastEvents() {
        int updated = eventRepository.markPastEventsCompleted(LocalDate.now());
        log.info("Auto-completed {} past events", updated);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────

    private Event getOwnedEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (!event.getOrganizer().getId().equals(organizerId))
            throw new UnauthorizedException("Unauthorized Access: you do not own this event");
        return event;
    }

    private Specification<Event> buildSpec(EventSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("visibility"), Event.EventVisibility.PUBLIC));
            if (Boolean.TRUE.equals(req.getPast())) {
                predicates.add(root.get("status").in(
                        Event.EventStatus.PUBLISHED,
                        Event.EventStatus.COMPLETED,
                        Event.EventStatus.EXPIRED));
                predicates.add(cb.or(
                        cb.lessThan(root.get("eventDate"), LocalDate.now()),
                        root.get("status").in(Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED)
                ));
            } else {
                predicates.add(root.get("status").in(Event.EventStatus.PUBLISHED));
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), LocalDate.now()));
            }

            if (StringUtils.hasText(req.getKeyword())) {
                String like = "%" + req.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("eventName")),   like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("tags")),        like),
                        cb.like(cb.lower(root.get("location")),    like),
                        cb.like(cb.lower(root.get("collegeName")), like),
                        cb.like(cb.lower(root.get("departmentName")), like)
                ));
            }
            if (StringUtils.hasText(req.getCategory()))
                predicates.add(cb.equal(cb.lower(root.get("category")), req.getCategory().toLowerCase()));
            if (StringUtils.hasText(req.getEventType()))
                predicates.add(cb.equal(cb.lower(root.get("eventType")), req.getEventType().toLowerCase()));
            if (StringUtils.hasText(req.getLocation()))
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + req.getLocation().toLowerCase() + "%"));
            if (StringUtils.hasText(req.getVenueName()))
                predicates.add(cb.like(cb.lower(root.get("venueName")), "%" + req.getVenueName().toLowerCase() + "%"));
            if (StringUtils.hasText(req.getCollegeName()))
                predicates.add(cb.like(cb.lower(root.get("collegeName")), "%" + req.getCollegeName().toLowerCase() + "%"));
            if (StringUtils.hasText(req.getDepartmentName()))
                predicates.add(cb.like(cb.lower(root.get("departmentName")), "%" + req.getDepartmentName().toLowerCase() + "%"));
            if (req.getDateFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), req.getDateFrom()));
            if (req.getDateTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), req.getDateTo()));
            if (req.getPriceMin() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("ticketPrice"), req.getPriceMin()));
            if (req.getPriceMax() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("ticketPrice"), req.getPriceMax()));
            if (Boolean.TRUE.equals(req.getFreeOnly()))
                predicates.add(cb.equal(root.get("ticketPrice"), java.math.BigDecimal.ZERO));
            if (Boolean.TRUE.equals(req.getPaidOnly()))
                predicates.add(cb.greaterThan(root.get("ticketPrice"), java.math.BigDecimal.ZERO));
            if (Boolean.TRUE.equals(req.getInterCollege()))
                predicates.add(cb.equal(cb.lower(root.get("category")), "inter_college"));
            if (Boolean.TRUE.equals(req.getIntraCollege()))
                predicates.add(cb.equal(cb.lower(root.get("category")), "intra_college"));
            if (StringUtils.hasText(req.getOrganizerName())) {
                Join<Object, Object> org = root.join("organizer", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(org.get("organizerName")), "%" + req.getOrganizerName().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by("eventDate").ascending();
        return switch (sortBy.toLowerCase()) {
            case "newest", "latest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "price_asc"  -> Sort.by("ticketPrice").ascending();
            case "price_desc" -> Sort.by("ticketPrice").descending();
            case "date_desc"  -> Sort.by("eventDate").descending();
            case "popular", "most_registered" -> Sort.by("availableSeats").ascending();
            default           -> Sort.by("eventDate").ascending();
        };
    }

    @Async
    protected void saveSearchHistory(Long userId, EventSearchRequest req, int resultCount) {
        if (searchHistoryRepository.isEmpty()) {
            log.warn("MongoDB is disabled or unavailable; skipping search history persistence");
            return;
        }
        try {
            searchHistoryRepository.get().save(SearchHistory.builder()
                    .userId(userId).keyword(req.getKeyword()).category(req.getCategory())
                    .eventType(req.getEventType()).location(req.getLocation())
                    .resultCount(resultCount).searchedAt(LocalDateTime.now()).build());
        } catch (Exception ex) {
            log.warn("Search history save failed: {}", ex.getMessage());
        }
    }

    private PagedResponse<EventResponse> toPagedResponse(Page<Event> page) {
        return PagedResponse.<EventResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .first(page.isFirst()).last(page.isLast()).build();
    }

    /** Delegates to EventMapper — kept for internal callers that use this::toResponse */
    public EventResponse toResponse(Event e) {
        return eventMapper.toResponse(e);
    }

    // ─── BANNER UPLOAD ────────────────────────────────────────────────────

    @Transactional
    public String uploadBanner(Long eventId, Long organizerId,
                                org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Event event = getOwnedEvent(eventId, organizerId);
        String ext = (file.getOriginalFilename() != null && file.getOriginalFilename().contains("."))
                ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                : ".jpg";
        String filename = "event_" + eventId + "_" + java.util.UUID.randomUUID() + ext;
        java.nio.file.Path dir = java.nio.file.Paths.get("uploads/banners");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.copy(file.getInputStream(), dir.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        event.setEventBanner("/uploads/banners/" + filename);
        eventRepository.save(event);
        return event.getEventBanner();
    }

    // ─── AUTHORIZED DOCUMENT UPLOAD ───────────────────────────────────────

    @Transactional
    public String uploadAuthorizedDocument(Long eventId, Long organizerId,
                                            org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Event event = getOwnedEvent(eventId, organizerId);
        if (file.isEmpty()) throw new com.eventbooking.exception.BookingException("Document is required");
        if (file.getSize() > 5 * 1024 * 1024) throw new com.eventbooking.exception.BookingException("Document must be 5MB or smaller");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        if (!java.util.List.of(".pdf", ".png", ".jpg", ".jpeg").contains(ext))
            throw new com.eventbooking.exception.BookingException("Supported formats: PDF, PNG, JPG, JPEG");
        String filename = "authorized_event_" + eventId + "_" + java.util.UUID.randomUUID() + ext;
        java.nio.file.Path dir = java.nio.file.Paths.get("uploads/authorized-documents");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.copy(file.getInputStream(), dir.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        String url = "/uploads/authorized-documents/" + filename;
        event.setAuthorizedDocumentUrl(url);
        eventRepository.save(event);
        return url;
    }
}
