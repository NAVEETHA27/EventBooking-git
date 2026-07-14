package com.eventbooking.service;

import com.eventbooking.entity.*;
import com.eventbooking.exception.BookingException;
import com.eventbooking.repository.*;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {
    private final CertificateRepository certificateRepository;
    private final CertificateSettingsRepository certificateSettingsRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final CertificateHistoryRepository certificateHistoryRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final GamificationService gamificationService;
    private final StorageService storageService;

    @Async
    @Transactional
    public void generateCertificatesForEvent(Long eventId) {
        generateCertificatesForEventInternal(eventId, null);
    }

    @Transactional
    public Map<String, Object> generateCertificatesForEventInternal(Long eventId, List<Long> userIds) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new BookingException("Event not found"));
        if (!event.isHasCertificate()) throw new BookingException("Certificates are not enabled for this event");
        if (event.getStatus() == Event.EventStatus.CANCELLED) throw new BookingException("Cancelled events cannot issue certificates");
        if (!isEventCompleted(event)) throw new BookingException("Certificates can be generated only after event completion");

        CertificateSettings settings = settingsFor(event);
        int generated = 0;
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (Booking booking : bookingRepository.findConfirmedWithDetailsByEventId(eventId)) {
            User user = booking.getUser();
            if (userIds != null && !userIds.contains(user.getId())) continue;
            if (certificateRepository.existsByEventIdAndUserId(eventId, user.getId())) continue;
            Eligibility eligibility = eligibility(event, booking, settings);
            if (!eligibility.eligible()) {
                skipped.add(Map.of("userId", user.getId(), "reason", eligibility.reason()));
                continue;
            }
            Certificate cert = buildCertificate(event, booking, settings);
            cert = certificateRepository.save(cert);
            cert.setQrCodeUrl(generateQrCode(cert));
            cert.setPdfUrl("/api/certificates/download/" + cert.getCertificateId());
            certificateRepository.save(cert);
            history(cert, "GENERATED", null, "SYSTEM", "Certificate generated");
            gamificationService.awardCertificateXp(user.getId());
            generated++;
        }
        return Map.of("generated", generated, "skipped", skipped);
    }

    @Transactional(readOnly = true)
    public Page<Certificate> getUserCertificates(Long userId, int page, int size) {
        return getUserCertificates(userId, page, size, null);
    }

    @Transactional(readOnly = true)
    public Page<Certificate> getUserCertificates(Long userId, int page, int size, String q) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("issuedAt").descending());
        if (q != null && !q.isBlank()) return certificateRepository.findByUserIdAndEventNameContainingIgnoreCase(userId, q, pr);
        return certificateRepository.findByUserId(userId, pr);
    }

    @Transactional
    public Certificate getCertificateById(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new BookingException("Certificate not found: " + certificateId));
        cert.setVerificationCount(cert.getVerificationCount() + 1);
        return certificateRepository.save(cert);
    }

    @Transactional(readOnly = true)
    public List<Certificate> getEventCertificates(Long eventId, Long organizerId) {
        ownedEvent(eventId, organizerId);
        return certificateRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEventParticipants(Long eventId, Long organizerId, String q, String department, String college) {
        Event event = ownedEvent(eventId, organizerId);
        CertificateSettings settings = settingsFor(event);
        return bookingRepository.findConfirmedWithDetailsByEventId(eventId).stream()
                .map(booking -> participantRow(event, booking, settings))
                .filter(row -> matches(row, q, department, college))
                .toList();
    }

    @Transactional
    public String uploadCertificateTemplate(Long eventId, Long organizerId, MultipartFile file, String signatureName) throws IOException {
        Event event = ownedEvent(eventId, organizerId);
        validateFile(file, List.of(".pdf", ".png", ".jpg", ".jpeg"), "Supported template formats: PDF, PNG, JPG, JPEG");
        if (file.getSize() > 10 * 1024 * 1024) throw new BookingException("Template must be 10MB or smaller");
        String templatePath = storageService.store("certificate-templates", file, "certificate_template_event_" + eventId);
        event.setCertificateTemplateUrl(templatePath);
        event.setCertificateSignatureName(signatureName);
        event.setHasCertificate(true);
        eventRepository.save(event);
        certificateTemplateRepository.findFirstByEventIdAndActiveTrueOrderByUploadedAtDesc(eventId).ifPresent(existing -> {
            existing.setActive(false);
            certificateTemplateRepository.save(existing);
        });
        certificateTemplateRepository.save(CertificateTemplate.builder()
                .event(event)
                .templateUrl(templatePath)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .active(true)
                .build());
        CertificateSettings settings = settingsFor(event);
        settings.setCertificateAvailable(true);
        certificateSettingsRepository.save(settings);
        return templatePath;
    }

    @Transactional
    public String uploadOrganizerSignature(Long eventId, Long organizerId, MultipartFile file) throws IOException {
        Event event = ownedEvent(eventId, organizerId);
        validateFile(file, List.of(".png", ".jpg", ".jpeg"), "Signature must be PNG, JPG, or JPEG");
        String path = storageService.store("certificate-signatures", file, "certificate_signature_event_" + eventId);
        CertificateSettings settings = settingsFor(event);
        settings.setOrganizerSignatureUrl(path);
        certificateSettingsRepository.save(settings);
        return path;
    }

    @Transactional
    public Map<String, Object> releaseCertificates(Long eventId, Long organizerId) {
        Event event = ownedEvent(eventId, organizerId);
        CertificateSettings settings = settingsFor(event);
        settings.setReleased(true);
        settings.setReleasedAt(LocalDateTime.now());
        certificateSettingsRepository.save(settings);
        int released = 0;
        for (Certificate cert : certificateRepository.findByEventId(eventId)) {
            if (cert.getStatus() == Certificate.CertificateStatus.REVOKED) continue;
            cert.setReleasedAt(LocalDateTime.now());
            cert.setStatus(Certificate.CertificateStatus.RELEASED);
            certificateRepository.save(cert);
            sendCertificateEmail(cert, cert.getUser(), event);
            released++;
        }
        return Map.of("released", released);
    }

    @Transactional
    public Certificate revokeCertificate(String certificateId, Long organizerId, String reason) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId).orElseThrow(() -> new BookingException("Certificate not found"));
        if (!cert.getEvent().getOrganizer().getId().equals(organizerId)) throw new BookingException("Access denied");
        cert.setStatus(Certificate.CertificateStatus.REVOKED);
        cert.setRevokedAt(LocalDateTime.now());
        cert.setRevokeReason(reason);
        history(cert, "REVOKED", organizerId, "ORGANIZER", reason);
        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate resendEmail(String certificateId, Long organizerId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId).orElseThrow(() -> new BookingException("Certificate not found"));
        if (!cert.getEvent().getOrganizer().getId().equals(organizerId)) throw new BookingException("Access denied");
        sendCertificateEmail(cert, cert.getUser(), cert.getEvent());
        return cert;
    }

    @Transactional(readOnly = true)
    public Page<Certificate> adminSearch(String q, int page, int size) {
        return certificateRepository.searchAll(q == null || q.isBlank() ? null : q, PageRequest.of(page, size, Sort.by("issuedAt").descending()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminStats() {
        List<Certificate> all = certificateRepository.findAll();
        return Map.of(
                "certificatesGenerated", all.size(),
                "certificatesDownloaded", all.stream().mapToLong(Certificate::getDownloadCount).sum(),
                "verificationRequests", all.stream().mapToLong(Certificate::getVerificationCount).sum(),
                "failedEmails", all.stream().filter(c -> !c.isEmailSent() && c.getEmailAttemptCount() > 0).count()
        );
    }

    @Transactional
    public void adminDeleteInvalid(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId).orElseThrow(() -> new BookingException("Certificate not found"));
        cert.setStatus(Certificate.CertificateStatus.REVOKED);
        cert.setRevokeReason("Revoked by admin");
        certificateRepository.save(cert);
        history(cert, "ADMIN_REVOKED", null, "ADMIN", "Revoked by admin");
    }

    @Transactional
    public byte[] downloadCertificatePdfForUser(String certificateId, Long userId, boolean admin) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId).orElseThrow(() -> new BookingException("Certificate not found"));
        if (!admin && !cert.getUser().getId().equals(userId)) throw new BookingException("Access denied");
        if (cert.getStatus() == Certificate.CertificateStatus.REVOKED) throw new BookingException("Certificate has been revoked");
        cert.setDownloadCount(cert.getDownloadCount() + 1);
        certificateRepository.save(cert);
        return generateCertificatePdfBytes(cert);
    }

    @Transactional(readOnly = true)
    public byte[] downloadCertificatePdf(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId).orElseThrow(() -> new BookingException("Certificate not found"));
        return generateCertificatePdfBytes(cert);
    }

    @Async
    protected void sendCertificateEmail(Certificate cert, User user, Event event) {
        try {
            if (cert.isEmailSent()) return;
            String subject = "Your Certificate for " + event.getEventName() + " is Ready";
            String body = """
                    Dear %s,

                    Congratulations! Your certificate for %s is ready.

                    Certificate ID: %s
                    Verification Link: %s
                    Download Link: /api/certificates/download/%s

                    Best regards,
                    CollegeEvents
                    """.formatted(user.getName(), event.getEventName(), cert.getCertificateId(), cert.getVerificationUrl(), cert.getCertificateId());
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
            cert.setEmailSent(true);
            cert.setEmailAttemptCount(cert.getEmailAttemptCount() + 1);
            cert.setEmailSentAt(LocalDateTime.now());
            cert.setStatus(Certificate.CertificateStatus.EMAILED);
            cert.setEmailFailureReason(null);
            certificateRepository.save(cert);
            history(cert, "EMAIL_SENT", null, "SYSTEM", "Certificate email sent");
        } catch (Exception ex) {
            cert.setEmailAttemptCount(cert.getEmailAttemptCount() + 1);
            cert.setEmailFailureReason(ex.getMessage());
            certificateRepository.save(cert);
            log.warn("Certificate email failed for {}: {}", cert.getCertificateId(), ex.getMessage());
        }
    }

    public byte[] generateCertificatePdfBytes(Certificate cert) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(com.itextpdf.kernel.geom.PageSize.A4.rotate());
            Document document = new Document(pdf);
            document.setMargins(40, 40, 40, 40);
            document.add(new Paragraph("COLLEGEEVENTS").setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("CERTIFICATE OF " + cert.getCertificateType().name().replace('_', ' ')).setFontSize(28).setBold().setTextAlignment(TextAlignment.CENTER).setMarginTop(28));
            document.add(new Paragraph("This certificate is proudly presented to").setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            document.add(new Paragraph(cert.getRecipientName()).setFontSize(26).setBold().setTextAlignment(TextAlignment.CENTER).setMarginTop(10));
            document.add(new Paragraph("for successfully participating in").setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginTop(15));
            document.add(new Paragraph(cert.getEventName()).setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER).setMarginTop(10));
            if (cert.getCollegeName() != null) document.add(new Paragraph(cert.getCollegeName()).setFontSize(12).setTextAlignment(TextAlignment.CENTER));

            Table table = new Table(3).useAllAvailableWidth().setMarginTop(40);
            Cell left = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            left.add(new Paragraph("Certificate ID: " + cert.getCertificateId()).setFontSize(10));
            left.add(new Paragraph("Issue Date: " + (cert.getIssuedAt() != null ? cert.getIssuedAt().toLocalDate() : "N/A")).setFontSize(10));
            left.add(new Paragraph("Event Date: " + (cert.getEvent() != null && cert.getEvent().getEventDate() != null ? cert.getEvent().getEventDate() : "N/A")).setFontSize(10));
            table.addCell(left);

            Cell middle = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            if (cert.getQrCodeUrl() != null) {
                String qrPath = cert.getQrCodeUrl().replaceFirst("^/", "");
                ImageData imgData = ImageDataFactory.create(qrPath);
                Image qrImage = new Image(imgData).setWidth(82).setHeight(82);
                qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                middle.add(qrImage);
                middle.add(new Paragraph("Scan to Verify").setFontSize(8));
            }
            table.addCell(middle);

            Cell right = new Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            right.add(new Paragraph(cert.getOrganizerName() != null ? cert.getOrganizerName() : "Authorized Organizer").setBold().setFontSize(12));
            right.add(new Paragraph(cert.getOrganizationName() != null ? cert.getOrganizationName() : "Organizer").setFontSize(10));
            table.addCell(right);
            document.add(table);
            document.add(new Paragraph(cert.getVerificationUrl()).setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating certificate PDF: {}", e.getMessage(), e);
            throw new BookingException("Failed to generate certificate PDF");
        }
    }

    private Certificate buildCertificate(Event event, Booking booking, CertificateSettings settings) {
        User user = booking.getUser();
        Participant participant = booking.getParticipants().isEmpty() ? null : booking.getParticipants().get(0);
        String certId = generateCertificateId();
        String recipientName = participant != null && participant.getName() != null && !participant.getName().isBlank() ? participant.getName() : user.getName();
        return Certificate.builder()
                .certificateId(certId)
                .event(event)
                .user(user)
                .recipientName(recipientName)
                .collegeName(participant != null ? participant.getCollege() : user.getOrganizationName())
                .departmentName(participant != null ? participant.getDepartment() : null)
                .eventName(event.getEventName())
                .participantId(user.getUserCode() != null ? user.getUserCode() : String.valueOf(user.getId()))
                .participantEmail(user.getEmail())
                .certificateType(settings.getCertificateType())
                .organizerName(settings.getOrganizerName())
                .organizationName(settings.getOrganizationName())
                .verificationUrl(verificationUrl(settings, certId))
                .releaseDate(resolveReleaseDate(event, settings))
                .issuedAt(LocalDateTime.now())
                .status(Certificate.CertificateStatus.GENERATED)
                .build();
    }

    private synchronized String generateCertificateId() {
        int year = Year.now().getValue();
        long next = certificateRepository.count() + 1;
        String id;
        do {
            id = "CERT-" + year + "-" + String.format("%06d", next++);
        } while (certificateRepository.findByCertificateId(id).isPresent());
        return id;
    }

    private Event ownedEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new BookingException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizerId)) throw new BookingException("Access denied");
        return event;
    }

    private CertificateSettings settingsFor(Event event) {
        return certificateSettingsRepository.findByEventId(event.getId()).orElseGet(() ->
                certificateSettingsRepository.save(CertificateSettings.builder()
                        .event(event)
                        .certificateAvailable(event.isHasCertificate())
                        .organizerName(event.getOrganizer().getOrganizerName())
                        .organizationName(event.getOrganizer().getOrganizationName())
                        .build()));
    }

    private boolean isEventCompleted(Event event) {
        if (event.getStatus() == Event.EventStatus.COMPLETED || event.getStatus() == Event.EventStatus.EXPIRED) return true;
        java.time.LocalDate end = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();
        if (end == null) return false;
        LocalTime endTime = event.getEndTime() != null ? event.getEndTime() : LocalTime.MAX;
        return LocalDateTime.of(end, endTime).isBefore(LocalDateTime.now());
    }

    private Eligibility eligibility(Event event, Booking booking, CertificateSettings settings) {
        if (booking.getBookingStatus() != Booking.BookingStatus.CONFIRMED) return new Eligibility(false, "Registration is not completed");
        if (booking.getTicketStatus() == Booking.TicketStatus.CANCELLED) return new Eligibility(false, "Ticket is cancelled");
        if (event.getStatus() == Event.EventStatus.CANCELLED) return new Eligibility(false, "Event is cancelled");
        if (!isEventCompleted(event)) return new Eligibility(false, "Event is not completed");
        if (booking.getAttendanceStatus() != Booking.AttendanceStatus.PRESENT || !booking.isCertificateEligible()) {
            return new Eligibility(false, "You were not marked present for this event.");
        }
        if (event.getTicketPrice() != null && event.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            boolean paid = booking.getPayments().stream().anyMatch(p -> p.getPaymentStatus() == Payment.PaymentStatus.SUCCESSFUL);
            if (!paid) return new Eligibility(false, "Payment is not successful");
        }
        if (!attendanceRepository.existsByBookingUserIdAndBookingEventId(booking.getUser().getId(), event.getId())) {
            return new Eligibility(false, "You were not marked present for this event.");
        }
        return new Eligibility(true, "Eligible");
    }

    private Map<String, Object> participantRow(Event event, Booking booking, CertificateSettings settings) {
        Participant participant = booking.getParticipants().isEmpty() ? null : booking.getParticipants().get(0);
        User user = booking.getUser();
        Eligibility eligibility = eligibility(event, booking, settings);
        Certificate cert = certificateRepository.findByEventIdAndUserId(event.getId(), user.getId()).orElse(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("participantName", participant != null ? participant.getName() : user.getName());
        row.put("participantId", user.getUserCode() != null ? user.getUserCode() : user.getId());
        row.put("email", user.getEmail());
        row.put("department", participant != null ? participant.getDepartment() : null);
        row.put("college", participant != null ? participant.getCollege() : user.getOrganizationName());
        row.put("eligible", eligibility.eligible());
        row.put("reason", eligibility.reason());
        row.put("certificateId", cert != null ? cert.getCertificateId() : null);
        row.put("certificateStatus", cert != null ? cert.getStatus() : null);
        return row;
    }

    private boolean matches(Map<String, Object> row, String q, String department, String college) {
        String haystack = row.values().toString().toLowerCase();
        if (q != null && !q.isBlank() && !haystack.contains(q.toLowerCase())) return false;
        if (department != null && !department.isBlank() && !String.valueOf(row.get("department")).toLowerCase().contains(department.toLowerCase())) return false;
        return college == null || college.isBlank() || String.valueOf(row.get("college")).toLowerCase().contains(college.toLowerCase());
    }

    private LocalDateTime resolveReleaseDate(Event event, CertificateSettings settings) {
        if (settings.getReleaseMode() == CertificateSettings.ReleaseMode.SCHEDULED && settings.getReleaseDate() != null) return settings.getReleaseDate().atStartOfDay();
        if (settings.getReleaseMode() == CertificateSettings.ReleaseMode.IMMEDIATE_AFTER_EVENT) {
            java.time.LocalDate end = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();
            return end != null ? end.atTime(event.getEndTime() != null ? event.getEndTime() : LocalTime.MAX) : LocalDateTime.now();
        }
        return null;
    }

    private String verificationUrl(CertificateSettings settings, String certId) {
        String base = settings.getVerificationBaseUrl();
        if (base == null || base.isBlank()) return "/verify/certificate/" + certId;
        return base.replaceAll("/$", "") + "/verify/certificate/" + certId;
    }

    private String generateQrCode(Certificate cert) {
        try {
            String qrPath = "uploads/qrcodes/qr_" + cert.getCertificateId() + ".png";
            Path path = Paths.get(qrPath);
            Files.createDirectories(path.getParent());
            com.google.zxing.qrcode.QRCodeWriter qrWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = qrWriter.encode(cert.getVerificationUrl(), com.google.zxing.BarcodeFormat.QR_CODE, 180, 180);
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            return "/uploads/qrcodes/qr_" + cert.getCertificateId() + ".png";
        } catch (Exception ex) {
            log.warn("Could not create certificate QR: {}", ex.getMessage());
            return null;
        }
    }

    private void history(Certificate cert, String action, Long actorId, String actorRole, String note) {
        certificateHistoryRepository.save(CertificateHistory.builder()
                .certificate(cert)
                .certificateId(cert.getCertificateId())
                .action(action)
                .actorId(actorId)
                .actorRole(actorRole)
                .note(note)
                .build());
    }

    private void validateFile(MultipartFile file, List<String> extensions, String message) {
        if (file == null || file.isEmpty()) throw new BookingException("File is required");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        if (!extensions.contains(ext)) throw new BookingException(message);
    }

    private record Eligibility(boolean eligible, String reason) {}
}
