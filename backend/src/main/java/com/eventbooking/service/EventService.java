package com.eventbooking.service;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.request.EventSearchRequest;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.PagedResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.dto.request.CertificateSettingsRequest;
import com.eventbooking.entity.CertificateSettings;
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
    private final StorageService            storageService;

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
            StorageService storageService,
            @Autowired(required = false) SearchHistoryRepository searchHistoryRepository) {
        this.eventRepository           = eventRepository;
        this.organizerRepository       = organizerRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.notificationService       = notificationService;
        this.emailService              = emailService;
        this.auditService              = auditService;
        this.eventMapper               = eventMapper;
        this.storageService            = storageService;
        this.searchHistoryRepository   = Optional.ofNullable(searchHistoryRepository);
    }

    // ─── CREATE ───────────────────────────────────────────────────────────

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
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
                .whatsappGroupLink(req.getWhatsappGroupLink())
                .whatsappContactNumber(req.getWhatsappContactNumber())
                .venueLatitude(req.getVenueLatitude())
                .venueLongitude(req.getVenueLongitude())
                .ticketPrice(req.getTicketPrice())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .status(Event.EventStatus.PUBLISHED)
                .visibility(req.getVisibility())
                .tags(req.getTags())
                .organizerDetails(req.getOrganizerDetails())
                .authorizedDocumentUrl(req.getAuthorizedDocumentUrl())
                .hasCertificate(Boolean.TRUE.equals(req.getHasCertificate()))
                .foodProvided(Boolean.TRUE.equals(req.getFoodProvided()))
                .foodMeals(req.getFoodMeals())
                .foodType(req.getFoodType())
                .teaCoffeeProvided(Boolean.TRUE.equals(req.getTeaCoffeeProvided()))
                .specialDiet(req.getSpecialDiet())
                .accommodationProvided(Boolean.TRUE.equals(req.getAccommodationProvided()))
                .accommodationType(req.getAccommodationType())
                .accommodationCharges(req.getAccommodationCharges())
                .accommodationBedsAvailable(req.getAccommodationBedsAvailable())
                .accommodationDetails(req.getAccommodationDetails())
                .boysHostelAvailable(Boolean.TRUE.equals(req.getBoysHostelAvailable()))
                .girlsHostelAvailable(Boolean.TRUE.equals(req.getGirlsHostelAvailable()))
                .hotelTieupAvailable(Boolean.TRUE.equals(req.getHotelTieupAvailable()))
                .accommodationCheckIn(req.getAccommodationCheckIn())
                .accommodationCheckOut(req.getAccommodationCheckOut())
                .accommodationContactPerson(req.getAccommodationContactPerson())
                .nearestBusStop(req.getNearestBusStop())
                .distanceFromBusStop(req.getDistanceFromBusStop())
                .busNumbers(req.getBusNumbers())
                .nearestRailwayStation(req.getNearestRailwayStation())
                .distanceFromRailwayStation(req.getDistanceFromRailwayStation())
                .nearestAirport(req.getNearestAirport())
                .metroInformation(req.getMetroInformation())
                .landmarks(req.getLandmarks())
                .parkingAvailable(req.getParkingAvailable())
                .travelGuide(req.getTravelGuide())
                .estimatedTravelTime(req.getEstimatedTravelTime())
                .cabEstimate(req.getCabEstimate())
                .nearbyHotels(req.getNearbyHotels())
                .nearbyRestaurants(req.getNearbyRestaurants())
                .emergencyContacts(req.getEmergencyContacts())
                .sessionSchedule(req.getSessionSchedule())
                .speakerList(req.getSpeakerList())
                .liveAnnouncements(req.getLiveAnnouncements())
                .reportingTime(req.getReportingTime())
                .dressCode(req.getDressCode())
                .itemsToBring(req.getItemsToBring())
                .laptopRequired(Boolean.TRUE.equals(req.getLaptopRequired()))
                .idCardRequired(Boolean.TRUE.equals(req.getIdCardRequired()))
                .teamSize(req.getTeamSize())
                .rules(req.getRules())
                .refundPolicy(req.getRefundPolicy())
                .cancellationPolicy(req.getCancellationPolicy())
                .certificateEligibility(req.getCertificateEligibility())
                .wifiAvailable(Boolean.TRUE.equals(req.getWifiAvailable()))
                .wheelchairAccessible(false)
                .restRoomsAvailable(false)
                .drinkingWaterAvailable(false)
                .medicalSupportAvailable(Boolean.TRUE.equals(req.getMedicalSupportAvailable()))
                .networkingEnabled(false)
                .build();

        event = eventRepository.save(event);
        applyCertificateSettings(event, req.getCertificateSettings(), organizer);
        event = eventRepository.save(event);

        // Auto-approve: create an approved ApprovalRequest so no admin action is needed
        ApprovalRequest autoApproval = ApprovalRequest.builder()
                .event(event)
                .organizer(organizer)
                .status(ApprovalRequest.ApprovalStatus.APPROVED)
                .reviewNote("Auto-approved on creation")
                .build();
        approvalRequestRepository.save(autoApproval);

        auditService.record(organizerId, "ORGANIZER", "EVENT_CREATED", "EVENT",
                String.valueOf(event.getId()), "Event created and auto-approved");
        log.info("Event created and auto-approved: {} by organizer {}", event.getId(), organizerId);
        return toResponse(event);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId"),
            @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    })
    public EventResponse updateEvent(Long eventId, Long organizerId, EventRequest req) {
        Event event = getOwnedEvent(eventId, organizerId);
        long confirmedBookings = eventRepository.countConfirmedBookings(event.getId());
        boolean hasRegistrations = confirmedBookings > 0
                || event.getAvailableSeats() < event.getTotalSeats();
        boolean published = List.of(
                Event.EventStatus.PUBLISHED,
                Event.EventStatus.UPCOMING,
                Event.EventStatus.LIVE,
                Event.EventStatus.ONGOING
        ).contains(event.getStatus());
        if (!published) {
            event.setEventName(req.getEventName());
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
            event.setVisibility(req.getVisibility());
            event.setTotalSeats(req.getTotalSeats());
            event.setAvailableSeats(req.getTotalSeats());
        } else if (!hasRegistrations && req.getEventDate() != null) {
            event.setEventDate(req.getEventDate());
            event.setEventTime(req.getEventTime());
            event.setEndDate(req.getEndDate());
            event.setEndTime(req.getEndTime());
            event.setRegistrationDeadline(req.getRegistrationDeadline());
        }
        event.setDescription(req.getDescription());
        event.setWhatsappGroupLink(req.getWhatsappGroupLink());
        event.setWhatsappContactNumber(req.getWhatsappContactNumber());
        event.setTags(req.getTags());
        event.setOrganizerDetails(req.getOrganizerDetails());
        if (StringUtils.hasText(req.getAuthorizedDocumentUrl())) {
            event.setAuthorizedDocumentUrl(req.getAuthorizedDocumentUrl());
        }
        if (!published) {
            event.setHasCertificate(Boolean.TRUE.equals(req.getHasCertificate()));
        }

        // Coordinates & Travel Info
        event.setVenueLatitude(req.getVenueLatitude());
        event.setVenueLongitude(req.getVenueLongitude());
        event.setNearestBusStop(req.getNearestBusStop());
        event.setDistanceFromBusStop(req.getDistanceFromBusStop());
        event.setBusNumbers(req.getBusNumbers());
        event.setNearestRailwayStation(req.getNearestRailwayStation());
        event.setDistanceFromRailwayStation(req.getDistanceFromRailwayStation());
        event.setNearestAirport(req.getNearestAirport());
        event.setMetroInformation(req.getMetroInformation());
        event.setLandmarks(req.getLandmarks());
        event.setParkingAvailable(req.getParkingAvailable());
        event.setTravelGuide(req.getTravelGuide());
        event.setEstimatedTravelTime(req.getEstimatedTravelTime());
        event.setCabEstimate(req.getCabEstimate());
        event.setNearbyHotels(req.getNearbyHotels());
        event.setNearbyRestaurants(req.getNearbyRestaurants());
        event.setEmergencyContacts(req.getEmergencyContacts());
        event.setSessionSchedule(req.getSessionSchedule());
        event.setSpeakerList(req.getSpeakerList());
        event.setLiveAnnouncements(req.getLiveAnnouncements());

        // Food & Accommodation
        event.setFoodProvided(Boolean.TRUE.equals(req.getFoodProvided()));
        event.setFoodMeals(req.getFoodMeals());
        event.setFoodType(req.getFoodType());
        event.setTeaCoffeeProvided(Boolean.TRUE.equals(req.getTeaCoffeeProvided()));
        event.setSpecialDiet(req.getSpecialDiet());
        event.setAccommodationProvided(Boolean.TRUE.equals(req.getAccommodationProvided()));
        event.setAccommodationType(req.getAccommodationType());
        event.setAccommodationCharges(req.getAccommodationCharges());
        event.setAccommodationBedsAvailable(req.getAccommodationBedsAvailable());
        event.setAccommodationDetails(req.getAccommodationDetails());
        event.setBoysHostelAvailable(Boolean.TRUE.equals(req.getBoysHostelAvailable()));
        event.setGirlsHostelAvailable(Boolean.TRUE.equals(req.getGirlsHostelAvailable()));
        event.setHotelTieupAvailable(Boolean.TRUE.equals(req.getHotelTieupAvailable()));
        event.setAccommodationCheckIn(req.getAccommodationCheckIn());
        event.setAccommodationCheckOut(req.getAccommodationCheckOut());
        event.setAccommodationContactPerson(req.getAccommodationContactPerson());

        // Important Information & Facilities
        event.setReportingTime(req.getReportingTime());
        event.setDressCode(req.getDressCode());
        event.setItemsToBring(req.getItemsToBring());
        event.setLaptopRequired(Boolean.TRUE.equals(req.getLaptopRequired()));
        event.setIdCardRequired(Boolean.TRUE.equals(req.getIdCardRequired()));
        event.setTeamSize(req.getTeamSize());
        event.setRules(req.getRules());
        event.setRefundPolicy(req.getRefundPolicy());
        event.setCancellationPolicy(req.getCancellationPolicy());
        event.setCertificateEligibility(req.getCertificateEligibility());
        event.setWifiAvailable(Boolean.TRUE.equals(req.getWifiAvailable()));
        event.setWheelchairAccessible(false);
        event.setRestRoomsAvailable(false);
        event.setDrinkingWaterAvailable(false);
        event.setMedicalSupportAvailable(Boolean.TRUE.equals(req.getMedicalSupportAvailable()));
        event.setNetworkingEnabled(false);
        if (!published) {
            applyCertificateSettings(event, req.getCertificateSettings(), event.getOrganizer());
        }

        // Auto-approve on re-edit after rejection
        if (event.getStatus() == Event.EventStatus.REJECTED) {
            event.setStatus(Event.EventStatus.PUBLISHED);
            ApprovalRequest reApproval = approvalRequestRepository
                    .findFirstByEventIdOrderByRequestedAtDesc(event.getId())
                    .orElseGet(() -> ApprovalRequest.builder()
                            .event(event)
                            .organizer(event.getOrganizer())
                            .build());
            reApproval.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
            reApproval.setReviewNote("Auto-approved after organizer re-submission");
            approvalRequestRepository.save(reApproval);
        }
        notificationService.notifyEventUpdate(event);
        auditService.record(organizerId, "ORGANIZER", "EVENT_EDITED", "EVENT",
                String.valueOf(event.getId()), "Event edited and auto-approved");
        return toResponse(eventRepository.save(event));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId"),
            @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    })
    public void deleteEvent(Long eventId, Long organizerId) {
        Event event = getOwnedEvent(eventId, organizerId);
        event.setStatus(Event.EventStatus.CANCELLED);
        eventRepository.save(event);
        log.info("Event deleted: {}", eventId);
    }

    // ─── CANCEL ───────────────────────────────────────────────────────────

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId"),
            @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    })
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
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId"),
            @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    })
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
    @org.springframework.cache.annotation.Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId"),
            @org.springframework.cache.annotation.CacheEvict(value = "recommendations", allEntries = true)
    })
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

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByStatus(List<Event.EventStatus> statuses) {
        return eventRepository.findByStatusIn(statuses).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId")
    public EventResponse forceExpireEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setStatus(Event.EventStatus.EXPIRED);
        auditService.record(userId, "ADMIN", "EVENT_FORCE_EXPIRED", "EVENT",
                String.valueOf(event.getId()), "Event force expired by admin");
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "eventCatalog", key = "#eventId")
    public EventResponse forceCompleteEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setStatus(Event.EventStatus.COMPLETED);
        auditService.record(userId, "USER", "EVENT_FORCE_COMPLETED", "EVENT",
                String.valueOf(event.getId()), "Event force completed");
        return toResponse(eventRepository.save(event));
    }

    // ─── GET ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "eventCatalog", key = "#eventId")
    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (event.getVisibility() != Event.EventVisibility.PUBLIC
                || event.getStatus() == Event.EventStatus.DRAFT) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }

        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventForViewer(Long eventId, Long viewerId, String role) {
        Event event = eventRepository.findByIdWithOrganizer(eventId)
                .or(() -> eventRepository.findById(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        boolean owner = viewerId != null
                && event.getOrganizer() != null
                && event.getOrganizer().getId().equals(viewerId)
                && "ORGANIZER".equalsIgnoreCase(role);
        boolean admin = "ADMIN".equalsIgnoreCase(role);
        if (!owner && !admin && (event.getVisibility() != Event.EventVisibility.PUBLIC
                || event.getStatus() == Event.EventStatus.DRAFT)) {
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
                        Event.EventStatus.LIVE,
                        Event.EventStatus.ONGOING,
                        Event.EventStatus.COMPLETED,
                        Event.EventStatus.EXPIRED));
                predicates.add(cb.or(
                        cb.lessThan(root.get("eventDate"), LocalDate.now()),
                        root.get("status").in(Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED)
                ));
            } else {
                predicates.add(root.get("status").in(
                        Event.EventStatus.PUBLISHED,
                        Event.EventStatus.LIVE,
                        Event.EventStatus.ONGOING,
                        Event.EventStatus.UPCOMING));
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

    private void applyCertificateSettings(Event event, CertificateSettingsRequest req, Organizer organizer) {
        boolean available = Boolean.TRUE.equals(event.isHasCertificate());
        CertificateSettings settings = event.getCertificateSettings();
        if (settings == null && (req != null || available)) {
            settings = CertificateSettings.builder().event(event).build();
            event.setCertificateSettings(settings);
        }
        if (settings == null) return;

        if (req != null) {
            available = Boolean.TRUE.equals(req.getCertificateAvailable()) || available;
            settings.setCertificateAvailable(available);
            settings.setAutomaticGeneration(Boolean.TRUE.equals(req.getAutomaticGeneration()));
            if (req.getReleaseMode() != null) settings.setReleaseMode(req.getReleaseMode());
            settings.setReleaseDate(req.getReleaseDate());
            settings.setMinimumAttendanceRequired(req.getMinimumAttendanceRequired() == null || Boolean.TRUE.equals(req.getMinimumAttendanceRequired()));
            if (req.getCertificateType() != null) settings.setCertificateType(req.getCertificateType());
            settings.setOrganizerName(StringUtils.hasText(req.getOrganizerName()) ? req.getOrganizerName() : organizer.getOrganizerName());
            settings.setOrganizationName(StringUtils.hasText(req.getOrganizationName()) ? req.getOrganizationName() : organizer.getOrganizationName());
            settings.setVerificationBaseUrl(req.getVerificationBaseUrl());
            settings.setCertificateExpiry(req.getCertificateExpiry());
            if (StringUtils.hasText(req.getTheme())) settings.setTheme(req.getTheme());
        } else {
            settings.setCertificateAvailable(available);
            settings.setOrganizerName(organizer.getOrganizerName());
            settings.setOrganizationName(organizer.getOrganizationName());
        }
        event.setHasCertificate(settings.isCertificateAvailable());
    }

    // ─── BANNER UPLOAD ────────────────────────────────────────────────────

    @Transactional
    public String uploadBanner(Long eventId, Long organizerId,
                                org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        Event event = getOwnedEvent(eventId, organizerId);
        String bannerPath = storageService.store("banners", file, "event_" + eventId);
        event.setEventBanner(bannerPath);
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
        String docPath = storageService.store("authorized-documents", file, "authorized_event_" + eventId);
        event.setAuthorizedDocumentUrl(docPath);
        eventRepository.save(event);
        return docPath;
    }
}
