package com.eventbooking.service;

import com.eventbooking.dto.request.ProfileLocationRequest;
import com.eventbooking.dto.response.OrganizerProfileResponse;
import com.eventbooking.dto.response.ProfileLocationResponse;
import com.eventbooking.entity.Organizer;
import com.eventbooking.exception.InvalidCredentialsException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerProfileService {

    private final OrganizerRepository    organizerRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuditService           auditService;
    private final ProfileLocationService profileLocationService;
    private final StorageService         storageService;

    @Transactional(readOnly = true)
    public OrganizerProfileResponse getProfile(Long organizerId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
        return OrganizerProfileResponse.from(org);
    }

    @Transactional
    public OrganizerProfileResponse updateProfile(Long organizerId, Map<String, String> body) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
        if (body.containsKey("organizerName") && StringUtils.hasText(body.get("organizerName")))
            org.setOrganizerName(StringUtils.trimWhitespace(body.get("organizerName")));
        if (body.containsKey("organizationName") && StringUtils.hasText(body.get("organizationName")))
            org.setOrganizationName(StringUtils.trimWhitespace(body.get("organizationName")));
        if (body.containsKey("phone"))     org.setPhone(StringUtils.trimWhitespace(body.get("phone")));
        if (body.containsKey("address"))   org.setAddress(StringUtils.trimWhitespace(body.get("address")));
        if (body.containsKey("pinCode"))   org.setPinCode(StringUtils.trimWhitespace(body.get("pinCode")));
        if (body.containsKey("city"))      org.setCity(StringUtils.trimWhitespace(body.get("city")));
        if (body.containsKey("state"))     org.setState(StringUtils.trimWhitespace(body.get("state")));
        if (body.containsKey("country"))   org.setCountry(StringUtils.trimWhitespace(body.get("country")));
        if (body.containsKey("website"))   org.setWebsite(StringUtils.trimWhitespace(body.get("website")));
        if (body.containsKey("description")) org.setDescription(StringUtils.trimWhitespace(body.get("description")));
        organizerRepository.save(org);
        auditService.record(organizerId, "ORGANIZER", "ACCOUNT_UPDATED", "ORGANIZER",
                String.valueOf(organizerId), "Organizer profile updated");
        return OrganizerProfileResponse.from(org);
    }

    public ProfileLocationResponse getLocation(Long organizerId) {
        return profileLocationService.get(organizerId, "ORGANIZER");
    }

    public ProfileLocationResponse updateLocation(Long organizerId, ProfileLocationRequest request) {
        return profileLocationService.save(organizerId, "ORGANIZER", request);
    }

    @Transactional
    public String uploadLogo(Long organizerId, MultipartFile file) throws IOException {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
        String logoPath = storageService.store("logos", file, "org_" + organizerId);
        org.setOrganizationLogo(logoPath);
        organizerRepository.save(org);
        auditService.record(organizerId, "ORGANIZER", "ORGANIZATION_LOGO_UPDATED", "ORGANIZER",
                String.valueOf(organizerId), "Organization logo updated");
        return org.getOrganizationLogo();
    }

    @Transactional
    public void changePassword(Long organizerId, String currentPassword, String newPassword) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
        if (!passwordEncoder.matches(currentPassword, org.getPasswordHash()))
            throw new InvalidCredentialsException("Current password is incorrect");
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8)
            throw new IllegalArgumentException("New password must be at least 8 characters");
        org.setPasswordHash(passwordEncoder.encode(newPassword));
        organizerRepository.save(org);
        auditService.record(organizerId, "ORGANIZER", "PASSWORD_CHANGED", "ORGANIZER",
                String.valueOf(organizerId), "Password changed");
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
