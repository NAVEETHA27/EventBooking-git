package com.eventbooking.ai.agent.tools;

import com.eventbooking.ai.agent.AgentTool;
import com.eventbooking.security.AuthPrincipal;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Email tool — surfaces email guidance and lets the agent inform the user
 * about automated notifications. Actual email dispatch goes through the
 * existing EmailService after user confirmation; this tool never sends
 * emails autonomously to prevent abuse.
 */
@Component
public class EmailTool implements AgentTool {

    @Override
    public String name() { return "emailTool"; }

    @Override
    public String description() {
        return "Provides information about automated email notifications (booking confirmation, event reminders, certificate ready). Can initiate email reminder requests that require organizer confirmation.";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, AuthPrincipal principal) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean isOrganizer = principal != null && "ORGANIZER".equalsIgnoreCase(principal.getRole());

        if (isOrganizer) {
            result.put("automatedEmails", java.util.List.of(
                    "Booking confirmation — sent immediately after successful payment",
                    "Event day reminder — sent at 6 AM on event day to all confirmed attendees",
                    "Certificate ready — sent after certificates are generated",
                    "Payment reminder — sent for pending bookings"
            ));
            result.put("customEmail", Map.of(
                    "available", true,
                    "note", "Custom email broadcasts to attendees require admin approval via the Notifications panel.",
                    "path", "/organizer/notifications"
            ));
        } else {
            result.put("automatedEmails", java.util.List.of(
                    "You receive a booking confirmation email after every successful payment",
                    "Event-day reminders are sent automatically at 6 AM",
                    "Certificate emails are sent when your certificate is ready to download"
            ));
            result.put("note", "Check your inbox or spam folder. All emails come from the platform's SMTP address.");
        }
        result.put("requiresConfirmation", false);
        return result;
    }
}
