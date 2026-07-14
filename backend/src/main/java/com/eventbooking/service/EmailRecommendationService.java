package com.eventbooking.service;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.User;
import com.eventbooking.entity.UserInterest;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.UserInterestRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 5 — AI Email Recommendation Engine.
 *
 * Sends weekly personalised event recommendation emails to all users.
 * Runs every Monday at 8 AM. Gracefully skips users if mail is not configured.
 * All email sending is async so scheduler thread is never blocked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRecommendationService {

    private final UserRepository           userRepository;
    private final EventRepository          eventRepository;
    private final UserInterestRepository   interestRepository;
    private final EmailService             emailService;

    // ── Weekly recommendation blast (every Monday 08:00) ─────────────────

    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklyRecommendations() {
        log.info("[EmailRec] Starting weekly recommendation emails");
        List<Event> upcoming = eventRepository.findUpcomingPublicEvents(LocalDate.now());
        if (upcoming.isEmpty()) {
            log.info("[EmailRec] No upcoming events — skipping weekly email blast");
            return;
        }

        List<User> users = userRepository.findAll();
        log.info("[EmailRec] Sending to {} users", users.size());

        for (User user : users) {
            try {
                sendRecommendationEmail(user, upcoming);
            } catch (Exception ex) {
                log.warn("[EmailRec] Failed for userId={}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("[EmailRec] Weekly recommendation emails queued");
    }

    // ── Event deadline reminder (daily 09:00) ────────────────────────────

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDeadlineReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Event> closingSoon = eventRepository.findUpcomingPublicEvents(LocalDate.now())
                .stream()
                .filter(e -> e.getRegistrationDeadline() != null
                          && e.getRegistrationDeadline().equals(tomorrow))
                .collect(Collectors.toList());

        if (closingSoon.isEmpty()) return;
        log.info("[EmailRec] {} events closing for registration tomorrow", closingSoon.size());

        userRepository.findAll().forEach(user -> {
            try {
                sendDeadlineReminderEmail(user, closingSoon);
            } catch (Exception ex) {
                log.warn("[EmailRec] Deadline reminder failed for userId={}: {}", user.getId(), ex.getMessage());
            }
        });
    }

    // ── Manual trigger (for admin use) ───────────────────────────────────

    @Async
    public void sendRecommendationToUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            List<Event> upcoming = eventRepository.findUpcomingPublicEvents(LocalDate.now());
            sendRecommendationEmail(user, upcoming);
        });
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    @Async
    protected void sendRecommendationEmail(User user, List<Event> allUpcoming) {
        UserInterest interest = interestRepository.findByUserId(user.getId()).orElse(null);

        // Pick up to 5 events relevant to this user's interests / department
        List<Event> picks = allUpcoming.stream()
                .filter(e -> isRelevant(e, interest))
                .limit(5)
                .collect(Collectors.toList());

        // Fall back to top 5 upcoming if no interest match
        if (picks.isEmpty()) {
            picks = allUpcoming.stream().limit(5).collect(Collectors.toList());
        }

        if (picks.isEmpty()) return;

        String eventRows = picks.stream().map(e -> """
                <tr>
                  <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0">
                    <strong style="color:#1565C0">%s</strong><br/>
                    <small style="color:#666">%s · %s · %s</small>
                  </td>
                  <td style="padding:10px 8px;border-bottom:1px solid #f0f0f0;text-align:right">
                    <a href="http://localhost:3000/events/%d"
                       style="background:#1565C0;color:white;padding:6px 14px;border-radius:6px;
                              text-decoration:none;font-size:12px">View</a>
                  </td>
                </tr>
                """.formatted(e.getEventName(),
                e.getCategory() != null ? e.getCategory().replace("_", " ") : "",
                e.getEventDate(),
                e.getTicketPrice() != null && e.getTicketPrice().signum() == 0 ? "Free"
                        : "₹" + e.getTicketPrice(),
                e.getId()))
                .collect(Collectors.joining());

        String greeting = interest != null && interest.getDepartment() != null
                ? "Based on your interest in <strong>" + interest.getDepartment() + "</strong>, we picked these for you:"
                : "Here are this week's top upcoming events:";

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:24px;background:#f9f9f9">
                  <div style="background:#1565C0;padding:20px;border-radius:8px 8px 0 0;text-align:center">
                    <h1 style="color:white;margin:0">🎓 CollegeEvents</h1>
                    <p style="color:#bbdefb;margin:4px 0 0">Your Weekly Event Picks</p>
                  </div>
                  <div style="background:white;padding:28px;border-radius:0 0 8px 8px">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>%s</p>
                    <table style="width:100%%;border-collapse:collapse;margin:16px 0">%s</table>
                    <p style="text-align:center;margin-top:20px">
                      <a href="http://localhost:3000/discover"
                         style="background:#1565C0;color:white;padding:12px 28px;border-radius:8px;
                                text-decoration:none;font-weight:bold">Discover All Events</a>
                    </p>
                    <p style="color:#999;font-size:12px;margin-top:20px">
                      You're receiving this because you have an account on CollegeEvents.
                    </p>
                  </div>
                </div>
                """.formatted(user.getName(), greeting, eventRows);

        emailService.sendSimpleEmail(user.getEmail(), "🎓 Your Weekly Event Picks — CollegeEvents", html);
        log.debug("[EmailRec] Sent weekly recommendation to {}", user.getEmail());
    }

    @Async
    protected void sendDeadlineReminderEmail(User user, List<Event> closingSoon) {
        String eventList = closingSoon.stream()
                .map(e -> "• " + e.getEventName() + " — " + e.getEventDate())
                .collect(Collectors.joining("\n"));

        emailService.sendSimpleEmail(
                user.getEmail(),
                "⏰ Registration Deadline Tomorrow — CollegeEvents",
                "Hi " + user.getName() + ",\n\nRegistration closes TOMORROW for:\n\n"
                        + eventList + "\n\nDon't miss out — register now at http://localhost:3000/discover"
        );
    }

    private boolean isRelevant(Event event, UserInterest interest) {
        if (interest == null) return true;
        String dept  = interest.getDepartment();
        String cats  = interest.getFavoriteCategories();
        String skills= interest.getSkills();

        if (dept != null && event.getDepartmentName() != null
                && event.getDepartmentName().toLowerCase().contains(dept.toLowerCase())) return true;
        if (cats != null && event.getCategory() != null) {
            for (String c : cats.split(",")) {
                if (event.getCategory().equalsIgnoreCase(c.trim())) return true;
            }
        }
        if (skills != null && event.getTags() != null) {
            for (String s : skills.split(",")) {
                if (event.getTags().toLowerCase().contains(s.trim().toLowerCase())) return true;
            }
        }
        return false;
    }
}
