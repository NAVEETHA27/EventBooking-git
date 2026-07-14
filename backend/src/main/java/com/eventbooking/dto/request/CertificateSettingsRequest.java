package com.eventbooking.dto.request;

import com.eventbooking.entity.CertificateSettings;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateSettingsRequest {
    private Boolean certificateAvailable;
    private Boolean automaticGeneration;
    private CertificateSettings.ReleaseMode releaseMode;
    private LocalDate releaseDate;
    private Boolean minimumAttendanceRequired;
    private CertificateSettings.CertificateType certificateType;
    private String organizerName;
    private String organizationName;
    private String verificationBaseUrl;
    private LocalDate certificateExpiry;
    private String theme;
}
