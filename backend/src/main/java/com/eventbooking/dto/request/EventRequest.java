package com.eventbooking.dto.request;

import com.eventbooking.entity.Event;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating / updating an event.
 * Uses plain @Data (no @Builder) so Jackson can deserialize correctly
 * without @Builder.Default / @AllArgsConstructor conflicts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 200)
    private String eventName;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Event type is required")
    private String eventType;

    // College-specific (optional)
    private String collegeName;
    private String departmentName;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    @NotNull(message = "Event time is required")
    private LocalTime eventTime;

    private LocalDate endDate;
    private LocalTime endTime;
    private LocalDate registrationDeadline;

    private String venueName;
    private String location;
    @Pattern(regexp = "^(|https?://.*)$", message = "Google Maps URL must start with http:// or https://")
    private String googleMapsUrl;
    @Pattern(regexp = "^(|https?://.*)$", message = "WhatsApp group link must start with http:// or https://")
    private String whatsappGroupLink;
    @Size(max = 30)
    private String whatsappContactNumber;
    private Double venueLatitude;
    private Double venueLongitude;

    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @NotNull(message = "Ticket price is required")
    private BigDecimal ticketPrice;

    @Min(value = 1, message = "Total seats must be at least 1")
    @NotNull(message = "Total seats is required")
    private Integer totalSeats;

    private String  tags;
    private String  organizerDetails;
    private String  authorizedDocumentUrl;
    private Boolean hasCertificate;
    private CertificateSettingsRequest certificateSettings;

    // ── Food (optional) ───────────────────────────────────────────────────
    private Boolean foodProvided;
    private String  foodMeals;
    private String  foodType;
    private Boolean teaCoffeeProvided;
    private String  specialDiet;

    // ── Accommodation (optional) ──────────────────────────────────────────
    private Boolean accommodationProvided;
    private String  accommodationType;
    private Integer accommodationCharges;
    private Integer accommodationBedsAvailable;
    private String  accommodationDetails;
    private Boolean boysHostelAvailable;
    private Boolean girlsHostelAvailable;
    private Boolean hotelTieupAvailable;
    private String  accommodationCheckIn;
    private String  accommodationCheckOut;
    private String  accommodationContactPerson;

    // ── Transportation (optional) ─────────────────────────────────────────
    private String nearestBusStop;
    private String distanceFromBusStop;
    private String busNumbers;
    private String nearestRailwayStation;
    private String distanceFromRailwayStation;
    private String nearestAirport;
    private String metroInformation;
    private String landmarks;
    private String parkingAvailable;
    private String travelGuide;
    private String estimatedTravelTime;
    private String cabEstimate;
    private String nearbyHotels;
    private String nearbyRestaurants;
    private String emergencyContacts;
    private String sessionSchedule;
    private String speakerList;
    private String liveAnnouncements;

    private LocalTime reportingTime;
    private String dressCode;
    private String itemsToBring;
    private Boolean laptopRequired;
    private Boolean idCardRequired;
    private String teamSize;
    private String rules;
    private String refundPolicy;
    private String cancellationPolicy;
    private String certificateEligibility;
    private Boolean wifiAvailable;
    private Boolean wheelchairAccessible;
    private Boolean restRoomsAvailable;
    private Boolean drinkingWaterAvailable;
    private Boolean medicalSupportAvailable;
    private Boolean networkingEnabled;

    // ── Status & Visibility — default to DRAFT/PUBLIC if not provided ──────
    private Event.EventStatus     status     = Event.EventStatus.DRAFT;
    private Event.EventVisibility visibility = Event.EventVisibility.PUBLIC;
}
