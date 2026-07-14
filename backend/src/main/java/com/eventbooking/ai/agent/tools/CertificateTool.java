package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.entity.Certificate;
import com.eventbooking.repository.CertificateRepository;
import com.eventbooking.security.AuthPrincipal;
import com.eventbooking.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.function.Function;

@Component("certificateTool")
@org.springframework.context.annotation.Description("Check certificate issue status and verification")
@RequiredArgsConstructor
public class CertificateTool implements AgentTool, Function<Map<String, Object>, Map<String, Object>> {

    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;

    @Override
    public String name() { return "certificateTool"; }

    @Override
    public String description() {
        return "Fetches user certificates, certificate status, and triggers certificate generation for completed events.";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (principal == null) {
            result.put("error", "Authentication required to view certificates.");
            return result;
        }
        try {
            boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(principal.getRole());

            if (isOrganizer) {
                Long eventId = longVal(input, "eventId");
                if (eventId != null) {
                    List<Certificate> certs = certificateService.getEventCertificates(eventId, principal.getId());
                    result.put("certificates", certs.stream().map(this::certMap).collect(Collectors.toList()));
                    result.put("count", certs.size());
                    result.put("eventId", eventId);
                } else {
                    result.put("message", "Provide eventId to list certificates for a specific event.");
                }
            } else {
                // Student: their own certificates
                List<Certificate> certs = certificateService
                        .getUserCertificates(principal.getId(), 0, 20)
                        .getContent();
                result.put("certificates", certs.stream().map(this::certMap).collect(Collectors.toList()));
                result.put("count", certs.size());
                long generated = certs.stream()
                        .filter(c -> c.getStatus() == Certificate.CertificateStatus.GENERATED).count();
                result.put("readyToDownload", generated);
            }
        } catch (Exception ex) {
            result.put("error", "Could not fetch certificates: " + ex.getMessage());
        }
        return result;
    }

    private Map<String, Object> certMap(Certificate c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificateId", c.getCertificateId());
        m.put("eventName", c.getEventName());
        m.put("issuedAt", c.getIssuedAt());
        m.put("status", c.getStatus());
        m.put("pdfUrl", c.getPdfUrl());
        m.put("emailSent", c.isEmailSent());
        return m;
    }

    private Long longVal(Map<String, Object> input, String key) {
        Object v = input.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {
        return execute(input, null);
    }
}
