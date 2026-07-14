package com.eventbooking.controller;

import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.Certificate;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.eventbooking.service.CertificateRecoveryService;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Certificate management for events")
public class CertificateController {
    private final CertificateService certificateService;
    private final CertificateRecoveryService recoveryService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<Certificate>>> myCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthPrincipal principal) {
        // Background recovery: generate any missing certificates for this user before returning
        try { recoveryService.recoverForUser(principal.getId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.success(certificateService.getUserCertificates(principal.getId(), page, size, q)));
    }

    @GetMapping("/verify/{certificateId}")
    @Operation(summary = "Verify a certificate by ID (public)")
    public ResponseEntity<ApiResponse<Certificate>> verify(@PathVariable String certificateId) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.getCertificateById(certificateId)));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<List<Certificate>>> eventCertificates(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.getEventCertificates(eventId, principal.getId())));
    }

    @GetMapping("/events/{eventId}/participants")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> participants(
            @PathVariable Long eventId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String college,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.getEventParticipants(eventId, principal.getId(), q, department, college)));
    }

    @PostMapping("/events/{eventId}/generate")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generate(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Certificate generation completed",
                certificateService.generateCertificatesForEventInternal(eventId, null)));
    }

    /**
     * Generate only MISSING certificates for an event (never re-generates existing ones).
     * Organizer action: "Generate Missing Certificates".
     */
    @PostMapping("/events/{eventId}/generate-missing")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMissing(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        // Verify ownership
        certificateService.getEventCertificates(eventId, principal.getId()); // throws if not owner
        int generated = recoveryService.recoverForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("Missing certificates generated",
                Map.of("generated", generated)));
    }

    @PostMapping("/events/{eventId}/generate-selected")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateSelected(
            @PathVariable Long eventId,
            @RequestBody Map<String, List<Long>> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        certificateService.getEventParticipants(eventId, principal.getId(), null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Selected certificate generation completed",
                certificateService.generateCertificatesForEventInternal(eventId, body.get("userIds"))));
    }

    @PostMapping("/events/{eventId}/release")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> release(
            @PathVariable Long eventId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Certificates released",
                certificateService.releaseCertificates(eventId, principal.getId())));
    }

    @PostMapping("/events/{eventId}/template")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<String>> uploadTemplate(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String signatureName,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Certificate template uploaded",
                certificateService.uploadCertificateTemplate(eventId, principal.getId(), file, signatureName)));
    }

    @PostMapping("/events/{eventId}/signature")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<String>> uploadSignature(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Signature uploaded",
                certificateService.uploadOrganizerSignature(eventId, principal.getId(), file)));
    }

    @PostMapping("/{certificateId}/email")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Certificate>> resendEmail(
            @PathVariable String certificateId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Certificate email queued",
                certificateService.resendEmail(certificateId, principal.getId())));
    }

    @PatchMapping("/{certificateId}/revoke")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Certificate>> revoke(
            @PathVariable String certificateId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked",
                certificateService.revokeCertificate(certificateId, principal.getId(), body != null ? body.get("reason") : null)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Certificate>>> adminSearch(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(certificateService.adminSearch(q, page, size)));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminStats() {
        return ResponseEntity.ok(ApiResponse.success(certificateService.adminStats()));
    }

    @DeleteMapping("/admin/{certificateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminDelete(@PathVariable String certificateId) {
        certificateService.adminDeleteInvalid(certificateId);
        return ResponseEntity.ok(ApiResponse.success("Certificate revoked", "OK"));
    }

    @GetMapping("/download/{certificateId}")
    @PreAuthorize("hasAnyRole('USER','ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> download(
            @PathVariable String certificateId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(principal.getRole())
                || "ORGANIZER".equalsIgnoreCase(principal.getRole());
        byte[] pdfBytes = certificateService.downloadCertificatePdfForUser(
                certificateId,
                principal.getId(),
                isPrivileged);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + certificateId + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
