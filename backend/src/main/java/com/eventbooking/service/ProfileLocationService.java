package com.eventbooking.service;

import com.eventbooking.dto.request.ProfileLocationRequest;
import com.eventbooking.dto.response.ProfileLocationResponse;
import com.eventbooking.entity.ProfileLocation;
import com.eventbooking.repository.ProfileLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileLocationService {
    private final ProfileLocationRepository profileLocationRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ProfileLocationResponse get(Long ownerId, String ownerType) {
        ProfileLocation location = "ORGANIZER".equals(ownerType)
                ? profileLocationRepository.findByOrganizerId(ownerId).orElse(null)
                : profileLocationRepository.findByUserId(ownerId).orElse(null);
        return ProfileLocationResponse.from(location);
    }

    @Transactional
    public ProfileLocationResponse save(Long ownerId, String ownerType, ProfileLocationRequest request) {
        ProfileLocation location = "ORGANIZER".equals(ownerType)
                ? profileLocationRepository.findByOrganizerId(ownerId).orElseGet(ProfileLocation::new)
                : profileLocationRepository.findByUserId(ownerId).orElseGet(ProfileLocation::new);
        if ("ORGANIZER".equals(ownerType)) {
            location.setOrganizerId(ownerId);
            location.setUserId(null);
        } else {
            location.setUserId(ownerId);
            location.setOrganizerId(null);
        }
        location.setAddress(request.getAddress());
        location.setStreet(request.getStreet());
        location.setArea(request.getArea());
        location.setCity(request.getCity());
        location.setDistrict(request.getDistrict());
        location.setState(request.getState());
        location.setCountry(request.getCountry());
        location.setPinCode(request.getPinCode());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        ProfileLocation saved = profileLocationRepository.save(location);
        auditService.record(ownerId, ownerType, "PROFILE_LOCATION_UPDATED", "PROFILE_LOCATION",
                String.valueOf(saved.getId()), "Profile location updated");
        return ProfileLocationResponse.from(saved);
    }
}
