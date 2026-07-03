package com.eventbooking.config;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.Faq;
import com.eventbooking.entity.Organizer;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.FaqRepository;
import com.eventbooking.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile("!prod")
public class MockDataSeeder {

    private static final String MOCK_EMAIL = "mock.organizer@collegeevents.local";
    private static final String MOCK_TAG = "MOCK_SEED_2026";

    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final FaqRepository faqRepository;

    @Bean
    CommandLineRunner seedMockEvents() {
        return args -> seed();
    }

    @Transactional
    public void seed() {
        Organizer organizer = organizerRepository.findByEmail(MOCK_EMAIL)
                .orElseGet(() -> organizerRepository.save(Organizer.builder()
                        .organizerCode("ORGMOCK")
                        .organizerName("Campus Events Office")
                        .organizationName("NovaTech Institute")
                        .email(MOCK_EMAIL)
                        .passwordHash("$2a$10$mockseedpasswordhashforlocaldataonly")
                        .phone("9876543210")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .country("India")
                        .emailVerified(true)
                        .approved(true)
                        .role(Organizer.OrganizerRole.ORGANIZER)
                        .build()));
        organizer.setOrganizationName("NovaTech Institute");
        organizerRepository.save(organizer);

        eventRepository.deleteByTagsContaining(MOCK_TAG);
        eventRepository.saveAll(mockEvents(organizer, LocalDate.now()));
        seedFaqs();
    }

    private List<Event> mockEvents(Organizer organizer, LocalDate today) {
        return List.of(
                event(organizer, "AI Hackathon 2026", "Build AI-powered campus solutions in a 24-hour team challenge with mentor support and final demos.", "HACKATHON", "OFFLINE", today.plusDays(7), LocalTime.of(9, 0), "Innovation Lab", "NovaTech Institute", "AI & Computer Science", new BigDecimal("250.00"), 120, true, true, "AI,Hackathon"),
                event(organizer, "Cloud Native Workshop", "Hands-on Docker, Kubernetes, and deployment workflow workshop for students preparing for modern backend roles.", "WORKSHOP", "OFFLINE", today.plusDays(10), LocalTime.of(10, 0), "Seminar Hall A", "Zenith College of Engineering", "Information Technology", new BigDecimal("150.00"), 80, false, true, "Cloud,Kubernetes,Workshop"),
                event(organizer, "Cyber Shield Seminar", "A focused seminar on phishing defense, secure coding basics, password hygiene, and career paths in cybersecurity.", "SEMINAR", "HYBRID", today.plusDays(13), LocalTime.of(14, 0), "Auditorium", "Vertex Institute of Technology", "Cyber Security", BigDecimal.ZERO, 250, true, true, "Cybersecurity,Seminar"),
                event(organizer, "Robo Race Challenge", "Design, tune, and race autonomous robots through a timed obstacle track.", "TECHNICAL_SYMPOSIUM", "OFFLINE", today.plusDays(18), LocalTime.of(9, 30), "Mechanical Workshop Arena", "Nexora Engineering College", "Mechanical Engineering", new BigDecimal("200.00"), 100, false, true, "Robotics,Technical"),
                event(organizer, "Cultural Night Auditions", "Open auditions for music, dance, theatre, and stand-up performances for the annual cultural night.", "CULTURAL", "OFFLINE", today.plusDays(22), LocalTime.of(16, 0), "Open Air Theatre", "Quantum Crest University", "Cultural Club", BigDecimal.ZERO, 300, false, false, "Cultural,Auditions"),
                event(organizer, "Full Stack Sprint", "A practical coding competition covering REST APIs, React UI flows, database design, and deployment basics.", "CODING_COMPETITION", "OFFLINE", today.plusDays(27), LocalTime.of(9, 0), "Computer Lab 3", "CodeVerse University", "AI & Computer Science", new BigDecimal("100.00"), 90, true, true, "Fullstack,Coding"),
                event(organizer, "Startup Pitch Day", "Student teams pitch startup ideas to mentors and alumni founders for feedback, networking, and incubation support.", "CLUB_ACTIVITY", "OFFLINE", today.plusDays(32), LocalTime.of(13, 0), "Incubation Centre", "Elevate Business School", "Business & Management", BigDecimal.ZERO, 110, true, true, "Startup,Pitch,ECell"),
                event(organizer, "Placement Aptitude Bootcamp", "A full-day bootcamp on quantitative aptitude, logical reasoning, resume hygiene, and interview preparation.", "PLACEMENT_PREP", "OFFLINE", today.plusDays(38), LocalTime.of(9, 30), "Training Hall", "Pinnacle Management Institute", "Business & Management", new BigDecimal("75.00"), 180, false, true, "Placement,Aptitude"),
                event(organizer, "IoT Project Expo", "Showcase sensor, automation, and embedded systems projects built by student teams.", "PROJECT_EXHIBITION", "OFFLINE", today.plusDays(44), LocalTime.of(10, 0), "ECE Project Gallery", "Orion Tech College", "Electronics and Communication", BigDecimal.ZERO, 160, true, true, "IoT,Expo,Projects"),
                event(organizer, "Data Analytics Technical Training", "Learn spreadsheet analytics, SQL querying, dashboarding, and storytelling with campus datasets.", "TECHNICAL_TRAINING", "HYBRID", today.plusDays(50), LocalTime.of(10, 0), "Analytics Lab", "DataSpring University", "Data Science", new BigDecimal("180.00"), 70, false, true, "Data,Analytics,Training"),
                event(organizer, "Inter College Quiz League", "Compete with teams from nearby colleges in technology, current affairs, science, and campus trivia rounds.", "INTER_COLLEGE", "OFFLINE", today.plusDays(57), LocalTime.of(11, 0), "Main Auditorium", "Lumina University", "Student Affairs", new BigDecimal("50.00"), 220, true, true, "Quiz,InterCollege"),
                event(organizer, "Intra College Sports Meet", "A campus sports meet with track events, volleyball, throwball, chess, and department-level team points.", "INTRA_COLLEGE", "OFFLINE", today.plusDays(65), LocalTime.of(8, 0), "College Sports Ground", "Veda Institute of Technology", "Physical Education", BigDecimal.ZERO, 500, false, false, "Sports,IntraCollege"),
                event(organizer, "Women in Tech Panel", "A panel discussion with alumni and industry mentors on technical careers, leadership, and community support.", "SEMINAR", "OFFLINE", today.plusDays(72), LocalTime.of(15, 0), "Mini Auditorium", "Neural Nexus College", "AI & Computer Science", BigDecimal.ZERO, 180, true, true, "WomenInTech,Seminar"),
                event(organizer, "Green Campus Innovation Workshop", "Brainstorm and prototype sustainability ideas for energy savings, waste reduction, and greener campus operations.", "WORKSHOP", "OFFLINE", today.plusDays(80), LocalTime.of(10, 0), "Civil Seminar Room", "SkyForge Institute", "Civil Engineering", new BigDecimal("60.00"), 75, false, true, "Sustainability,Workshop"),
                event(organizer, "AR/VR Experience Day", "Explore immersive demos and mini-builds using augmented and virtual reality tools for education and entertainment.", "TECHNICAL_SYMPOSIUM", "HYBRID", today.plusDays(88), LocalTime.of(11, 0), "Media Lab", "AstraNova University", "AI & Computer Science", new BigDecimal("120.00"), 95, true, true, "AR,VR,Technical")
        );
    }

    private Event event(Organizer organizer, String name, String description, String category, String type,
                        LocalDate date, LocalTime time, String venue, String college, String department, BigDecimal price,
                        int seats, boolean featured, boolean certificate, String tags) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return Event.builder()
                .organizer(organizer)
                .eventName(name)
                .description(description)
                .category(category)
                .eventType(type)
                .collegeName(college)
                .departmentName(department)
                .eventBanner("/uploads/banners/mock-" + slug + ".jpg")
                .authorizedDocumentUrl("/uploads/authorized-documents/mock-" + slug + "-approval.pdf")
                .eventDate(date)
                .eventTime(time)
                .endDate(date)
                .endTime(time.plusHours(4))
                .registrationDeadline(date.minusDays(2))
                .venueName(venue)
                .location(college + ", Chennai")
                .googleMapsUrl("https://maps.google.com/?q=" + college.replace(" ", "+") + "+Chennai")
                .ticketPrice(price)
                .totalSeats(seats)
                .availableSeats(seats)
                .status(Event.EventStatus.PUBLISHED)
                .visibility(Event.EventVisibility.PUBLIC)
                .featured(featured)
                .hasCertificate(certificate)
                .tags(tags + "," + MOCK_TAG)
                .organizerDetails("Organization type: College")
                .build();
    }

    private void seedFaqs() {
        List<Faq> faqs = List.of(
                faq("Booking", "How do I register for an event?", "Open Discover Events, choose an active event, select ticket quantity, fill participant details, and complete payment if the event is paid."),
                faq("Booking", "Can I book tickets for multiple participants?", "Yes. Choose the quantity first. The registration page will show one participant form for each ticket."),
                faq("Booking", "Why is the Register button disabled?", "Registration is disabled when the event is cancelled, completed, expired, sold out, or not published."),
                faq("Tickets", "Where can I view my ticket?", "Go to My Bookings and open the booking detail page. Active confirmed bookings show the ticket QR code."),
                faq("Tickets", "What happens when a ticket is cancelled?", "Cancelled tickets stay in your booking history for audit purposes and are marked as Ticket Cancelled with the cancellation timestamp."),
                faq("Tickets", "What does Ticket Expired mean?", "A ticket expires when payment is not completed in time or when lifecycle rules mark it as expired. Expired tickets cannot be used for entry."),
                faq("Payments", "How do free event bookings work?", "Free events skip paid checkout and can be confirmed without collecting a payment amount."),
                faq("Payments", "How do refunds work?", "When a successful paid booking is cancelled, a refund request is created and can be tracked from the refund section."),
                faq("Organizer", "How does an organizer create an event?", "Organizers can create events from the organizer dashboard, add event details, upload a poster and authorized organization document, then submit or publish based on approval flow."),
                faq("Organizer", "What is the authorized organization document?", "It is an approval letter, permission note, or official document proving the organization is authorized to conduct the event."),
                faq("Organizer", "Can organizers see registered participants?", "Yes. The organizer attendees page lists participants, emails, departments, colleges, event names, and booking references."),
                faq("Search", "How do I find hackathons or workshops?", "Use Discover Events filters for category, event type, college, department, venue, location, date range, free or paid events, and sorting."),
                faq("Search", "Why are expired events not visible in Discover?", "Discover only shows active upcoming published events. Expired and completed events belong in Past Events."),
                faq("Profile", "Where can I see my user ID?", "Open your profile page. User and organizer IDs are shown there and cannot be edited."),
                faq("Support", "Who can I contact if my booking fails?", "Check your booking status first. If payment or ticket status looks incorrect, contact the event organizer or platform admin with your booking ID.")
        );

        faqs.stream()
                .filter(faq -> !faqRepository.existsByQuestionIgnoreCase(faq.getQuestion()))
                .forEach(faqRepository::save);
    }

    private Faq faq(String category, String question, String answer) {
        return Faq.builder()
                .category(category)
                .question(question)
                .answer(answer)
                .active(true)
                .build();
    }
}
