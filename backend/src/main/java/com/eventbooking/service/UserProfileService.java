package com.eventbooking.service;

import com.eventbooking.dto.request.ProfileLocationRequest;
import com.eventbooking.dto.response.ProfileLocationResponse;
import com.eventbooking.dto.response.UserProfileResponse;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.UserRepository;
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
public class UserProfileService {

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuditService           auditService;
    private final ProfileLocationService profileLocationService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (body.containsKey("name") && StringUtils.hasText(body.get("name")))
            user.setName(StringUtils.trimWhitespace(body.get("name")));
        if (body.containsKey("phone"))
            user.setPhone(StringUtils.trimWhitespace(body.get("phone")));
        if (body.containsKey("address"))
            user.setAddress(StringUtils.trimWhitespace(body.get("address")));
        if (body.containsKey("pinCode"))
            user.setPinCode(StringUtils.trimWhitespace(body.get("pinCode")));
        if (body.containsKey("organizationName"))
            user.setOrganizationName(StringUtils.trimWhitespace(body.get("organizationName")));
        if (body.containsKey("city"))
            user.setCity(StringUtils.trimWhitespace(body.get("city")));
        if (body.containsKey("state"))
            user.setState(StringUtils.trimWhitespace(body.get("state")));
        if (body.containsKey("country"))
            user.setCountry(StringUtils.trimWhitespace(body.get("country")));
        if (body.containsKey("gender"))
            user.setGender(body.get("gender"));
        userRepository.save(user);
        auditService.record(userId, "USER", "ACCOUNT_UPDATED", "USER",
                String.valueOf(userId), "User profile updated");
        return UserProfileResponse.from(user);
    }

    public ProfileLocationResponse getLocation(Long userId) {
        return profileLocationService.get(userId, "USER");
    }

    public ProfileLocationResponse updateLocation(Long userId, ProfileLocationRequest request) {
        return profileLocationService.save(userId, "USER", request);
    }

    @Transactional
    public String uploadPicture(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String ext = getExt(file.getOriginalFilename());
        String filename = "user_" + userId + "_" + UUID.randomUUID() + ext;
        Path dir = Paths.get("uploads/profile");
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        user.setProfilePicture("/uploads/profile/" + filename);
        userRepository.save(user);
        auditService.record(userId, "USER", "PROFILE_PHOTO_UPDATED", "USER",
                String.valueOf(userId), "Profile photo updated");
        return user.getProfilePicture();
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new com.eventbooking.exception.InvalidCredentialsException("Current password is incorrect");
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8)
            throw new IllegalArgumentException("New password must be at least 8 characters");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.record(userId, "USER", "PASSWORD_CHANGED", "USER",
                String.valueOf(userId), "Password changed");
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
