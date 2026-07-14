package com.eventbooking.service;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.Participant;
import com.eventbooking.exception.BookingException;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventAssetService {
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportParticipants(Long eventId, Long organizerId) {
        Event event = ownedEvent(eventId, organizerId);
        List<Participant> participants = participantRepository.findByEventOrganizerIdWithDetails(organizerId).stream()
                .filter(participant -> participant.getEvent().getId().equals(eventId))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Event,Date,Ticket ID,Participant Name,Email,Department,College,Booking ID\n");
        for (Participant participant : participants) {
            csv.append(cell(event.getEventName())).append(',')
                    .append(cell(String.valueOf(event.getEventDate()))).append(',')
                    .append(cell(participant.getBooking() != null ? participant.getBooking().getTicketId() : "")).append(',')
                    .append(cell(participant.getName())).append(',')
                    .append(cell(participant.getEmail())).append(',')
                    .append(cell(participant.getDepartment())).append(',')
                    .append(cell(participant.getCollege())).append(',')
                    .append(cell(participant.getBooking() != null ? String.valueOf(participant.getBooking().getId()) : ""))
                    .append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "participants_event_" + eventId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @Transactional(readOnly = true)
    public String generatePoster(Long eventId, Long organizerId) throws IOException {
        Event event = ownedEvent(eventId, organizerId);
        Path dir = Paths.get("uploads/posters");
        Files.createDirectories(dir);
        String filename = "poster_event_" + eventId + "_" + UUID.randomUUID() + ".svg";
        Files.writeString(dir.resolve(filename), posterSvg(event), StandardCharsets.UTF_8);
        return "/uploads/posters/" + filename;
    }

    private Event ownedEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BookingException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new BookingException("Access denied");
        }
        return event;
    }

    private String posterSvg(Event event) {
        String date = event.getEventDate() != null
                ? event.getEventDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Date TBA";
        String price = event.getTicketPrice() == null || event.getTicketPrice().signum() == 0
                ? "FREE"
                : "INR " + event.getTicketPrice();
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1350" viewBox="0 0 1080 1350">
                  <defs>
                    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="#0f172a"/>
                      <stop offset="0.55" stop-color="#0f766e"/>
                      <stop offset="1" stop-color="#1d4ed8"/>
                    </linearGradient>
                  </defs>
                  <rect width="1080" height="1350" fill="url(#bg)"/>
                  <rect x="70" y="70" width="940" height="1210" rx="42" fill="rgba(255,255,255,0.92)"/>
                  <text x="120" y="170" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#0f766e">%s</text>
                  <text x="120" y="310" font-family="Arial, sans-serif" font-size="78" font-weight="900" fill="#0f172a">%s</text>
                  <text x="120" y="390" font-family="Arial, sans-serif" font-size="34" fill="#334155">%s</text>
                  <rect x="120" y="500" width="840" height="3" fill="#cbd5e1"/>
                  <text x="120" y="610" font-family="Arial, sans-serif" font-size="42" font-weight="700" fill="#0f172a">%s</text>
                  <text x="120" y="680" font-family="Arial, sans-serif" font-size="32" fill="#475569">%s</text>
                  <text x="120" y="750" font-family="Arial, sans-serif" font-size="32" fill="#475569">%s</text>
                  <text x="120" y="820" font-family="Arial, sans-serif" font-size="32" fill="#475569">Seats: %s</text>
                  <rect x="120" y="940" width="300" height="94" rx="20" fill="#0f766e"/>
                  <text x="160" y="1002" font-family="Arial, sans-serif" font-size="34" font-weight="800" fill="#ffffff">%s</text>
                  <text x="120" y="1160" font-family="Arial, sans-serif" font-size="26" fill="#64748b">Generated by EventGPT</text>
                </svg>
                """.formatted(
                escape(event.getCategory()),
                escape(event.getEventName()),
                escape(event.getCollegeName()),
                escape(date + " at " + (event.getEventTime() != null ? event.getEventTime() : "Time TBA")),
                escape(event.getVenueName() != null ? event.getVenueName() : event.getLocation()),
                escape(event.getDepartmentName()),
                event.getAvailableSeats() + "/" + event.getTotalSeats(),
                escape(price));
    }

    private String cell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
