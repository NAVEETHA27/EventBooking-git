package com.eventbooking.dto.response;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.CertificateSettings;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventResponse {
    private Long id;
    private String eventName;
    private String description;
    private String category;
    private String eventType;
    private String collegeName;
    private String departmentName;
    private String eventBanner;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private LocalDate registrationDeadline;
    private String venueName;
    private String location;
    private String googleMapsUrl;
    private String whatsappGroupLink;
    private String whatsappContactNumber;
    private Double venueLatitude;
    private Double venueLongitude;
    private BigDecimal ticketPrice;
    private int totalSeats;
    private int availableSeats;
    private Event.EventStatus status;
    private Event.EventVisibility visibility;
    private boolean featured;
    private boolean hasCertificate;
    private CertificateSettingsInfo certificateSettings;
    private String tags;
    private String organizerDetails;
    private String authorizedDocumentUrl;
    private OrganizerInfo organizer;
    private LocalDateTime createdAt;
    private LocalDate certificateDeadline;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;
    // Food & Accommodation
    private boolean foodProvided;
    private String  foodMeals;
    private String  foodType;
    private boolean teaCoffeeProvided;
    private String  specialDiet;
    private boolean accommodationProvided;
    private String  accommodationType;
    private Integer accommodationCharges;
    private Integer accommodationBedsAvailable;
    private String  accommodationDetails;
    private boolean boysHostelAvailable;
    private boolean girlsHostelAvailable;
    private boolean hotelTieupAvailable;
    private String accommodationCheckIn;
    private String accommodationCheckOut;
    private String accommodationContactPerson;
    // Transportation
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
    private boolean laptopRequired;
    private boolean idCardRequired;
    private String teamSize;
    private String rules;
    private String refundPolicy;
    private String cancellationPolicy;
    private String certificateEligibility;
    private boolean wifiAvailable;
    private boolean wheelchairAccessible;
    private boolean restRoomsAvailable;
    private boolean drinkingWaterAvailable;
    private boolean medicalSupportAvailable;
    private boolean networkingEnabled;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrganizerInfo {
        private Long id;
        private String organizerName;
        private String organizationName;
        private String organizationLogo;
        private String email;
        private String phone;
        private String organizerCode;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CertificateSettingsInfo {
        private boolean certificateAvailable;
        private boolean automaticGeneration;
        private CertificateSettings.ReleaseMode releaseMode;
        private LocalDate releaseDate;
        private boolean minimumAttendanceRequired;
        private CertificateSettings.CertificateType certificateType;
        private String organizerName;
        private String organizationName;
        private String verificationBaseUrl;
        private LocalDate certificateExpiry;
        private String theme;
        private boolean released;
        private LocalDateTime releasedAt;
    }
}
