package com.eventbooking.controller;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.request.EventSearchRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.PagedResponse;
import com.eventbooking.exception.BookingException;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ── Public ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<EventResponse>>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String venueName,
            @RequestParam(required = false) String organizerName,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(required = false) Boolean paidOnly,
            @RequestParam(required = false) Boolean interCollege,
            @RequestParam(required = false) Boolean intraCollege,
            @RequestParam(required = false) Boolean past,
            @RequestParam(defaultValue = "date_asc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {

        EventSearchRequest req = EventSearchRequest.builder()
                .keyword(keyword).category(category).eventType(eventType)
                .location(location).collegeName(collegeName).departmentName(departmentName)
                .venueName(venueName).organizerName(organizerName)
                .dateFrom(parseDate(dateFrom)).dateTo(parseDate(dateTo))
                .priceMin(priceMin).priceMax(priceMax)
                .freeOnly(freeOnly).paidOnly(paidOnly)
                .interCollege(interCollege).intraCollege(intraCollege).past(past)
                .sortBy(sortBy).page(page).size(size).build();

        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(eventService.search(req, userId)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<EventResponse>>> featured() {
        return ResponseEntity.ok(ApiResponse.success(eventService.getFeaturedEvents()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(eventService.getEvent(id)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                "HACKATHON", "TECHNICAL_SYMPOSIUM", "CODING_COMPETITION",
                "WORKSHOP", "SEMINAR", "PROJECT_EXHIBITION",
                "PLACEMENT_PREP", "TECHNICAL_TRAINING",
                "CULTURAL", "SPORTS", "CLUB_ACTIVITY",
                "DEPARTMENT_FUNCTION", "INTER_COLLEGE", "INTRA_COLLEGE", "OTHER")));
    }

    // ── Organizer ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully",
                        eventService.createEvent(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Event updated",
                eventService.updateEvent(id, principal.getId(), request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        eventService.deleteEvent(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Event cancelled",
                eventService.cancelEvent(id, principal.getId())));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Event published",
                eventService.publishEvent(id, principal.getId())));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<PagedResponse<EventResponse>>> myEvents(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                eventService.getOrganizerEvents(principal.getId(), status, page, size)));
    }

    @PostMapping("/{id}/banner")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<String>> uploadBanner(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Banner uploaded",
                eventService.uploadBanner(id, principal.getId(), file)));
    }

    @PostMapping("/{id}/authorized-document")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<String>> uploadAuthorizedDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Authorized document uploaded",
                eventService.uploadAuthorizedDocument(id, principal.getId(), file)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BookingException("Invalid date format — use YYYY-MM-DD");
        }
    }
}
