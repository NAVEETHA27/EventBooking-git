package com.eventbooking.config;

import com.eventbooking.entity.*;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Profile("!prod")
public class MockDataSeeder {

    private static final String MOCK_EMAIL = "mock.organizer@collegeevents.local";
    private static final String MOCK_TAG   = "MOCK_SEED_2026";
    private static final String USER_EMAIL = "demo.student@collegeevents.local";

    private final OrganizerRepository   organizerRepository;
    private final EventRepository       eventRepository;
    private final FaqRepository         faqRepository;
    private final UserRepository        userRepository;
    private final BookingRepository     bookingRepository;
    private final ParticipantRepository participantRepository;
    private final CertificateRepository certificateRepository;
    private final PaymentRepository     paymentRepository;
    private final RefundRepository      refundRepository;

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
                        .phone("9876543210").city("Chennai").state("Tamil Nadu").country("India")
                        .emailVerified(true).approved(true)
                        .role(Organizer.OrganizerRole.ORGANIZER).build()));
        organizer.setOrganizationName("NovaTech Institute");
        organizerRepository.save(organizer);
        eventRepository.clearRemovedFacilityFlags();

        // Demo user for bookings/certificates
        User demoUser = userRepository.findByEmail(USER_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .userCode("USRDEMO")
                        .name("Demo Student")
                        .email(USER_EMAIL)
                        .passwordHash("$2a$10$mockseedpasswordhashforlocaldataonly")
                        .organizationName("NovaTech Institute")
                        .city("Chennai").emailVerified(true)
                        .role(User.UserRole.USER).build()));

        mockEvents(organizer, LocalDate.now()).stream()
                .filter(event -> !eventRepository.existsByEventName(event.getEventName()))
                .forEach(eventRepository::save);

        seedComprehensiveMockEvents(organizer);
        refreshMockPosterOverrides();

        // Seed completed events with bookings + certificates
        seedCompletedEventsWithCerts(organizer, demoUser);

        seedFaqs();
    }

    private void seedCompletedEventsWithCerts(Organizer organizer, User user) {
        LocalDate past = LocalDate.now().minusDays(10);
        // Create 3 completed past events with certificates
        String[][] completedEvents = {
            {"AI Workshop Completed", "WORKSHOP", "Hands-on AI workshop that was completed.", "AI,Workshop,MOCK_COMPLETED"},
            {"Hackathon 2026 Finals", "HACKATHON", "National hackathon finals — completed.", "Hackathon,Finals,MOCK_COMPLETED"},
            {"Cloud Seminar Completed", "SEMINAR", "Cloud computing seminar — completed.", "Cloud,Seminar,MOCK_COMPLETED"}
        };

        for (String[] ev : completedEvents) {
            if (eventRepository.existsByEventName(ev[0])) continue;

            Event event = eventRepository.save(Event.builder()
                    .organizer(organizer).eventName(ev[0]).description(ev[2])
                    .category(ev[1]).eventType("OFFLINE")
                    .collegeName("NovaTech Institute").departmentName("CSE")
                    .eventDate(past).eventTime(LocalTime.of(9, 0))
                    .endDate(past).endTime(LocalTime.of(17, 0))
                    .venueName("NovaTech Auditorium").location("NovaTech Institute, Chennai")
                    .ticketPrice(new BigDecimal("199")).totalSeats(100).availableSeats(70)
                    .status(Event.EventStatus.COMPLETED).visibility(Event.EventVisibility.PUBLIC)
                    .hasCertificate(true).certificateDeadline(past.plusDays(7))
                    .completedAt(past.atTime(17, 0)).foodProvided(false).accommodationProvided(false)
                    .tags(ev[3] + "," + MOCK_TAG).featured(false).build());

            // Confirmed booking for demo user
            String ticketId = "TKT-DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Booking booking = bookingRepository.save(Booking.builder()
                    .ticketId(ticketId).user(user).event(event)
                    .quantity(1).totalAmount(new BigDecimal("199"))
                    .bookingStatus(Booking.BookingStatus.CONFIRMED)
                    .ticketStatus(Booking.TicketStatus.ACTIVE).build());

            participantRepository.save(Participant.builder()
                    .booking(booking).event(event)
                    .name(user.getName()).email(user.getEmail())
                    .department("CSE").college("NovaTech Institute").build());

            paymentRepository.save(Payment.builder()
                    .transactionId("TXN-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .booking(booking).amount(new BigDecimal("199"))
                    .paymentStatus(Payment.PaymentStatus.SUCCESSFUL)
                    .paymentMethod("RAZORPAY")
                    .gatewayReference("GW-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build());

            // Generate certificate directly from booking (bypass attendance requirement for mock data)
            String certId = "CERT-MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            if (!certificateRepository.existsByEventIdAndUserId(event.getId(), user.getId())) {
                certificateRepository.save(Certificate.builder()
                        .certificateId(certId)
                        .event(event).user(user)
                        .recipientName(user.getName())
                        .collegeName("NovaTech Institute")
                        .departmentName("CSE")
                        .eventName(event.getEventName())
                        .issuedAt(LocalDateTime.now())
                        .status(Certificate.CertificateStatus.GENERATED)
                        .emailSent(false).build());
            }
        }
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
                .eventBanner("https://picsum.photos/seed/" + slug + "/800/400")
                .authorizedDocumentUrl(null)
                .eventDate(date)
                .eventTime(time)
                .endDate(date)
                .endTime(time.plusHours(4))
                .registrationDeadline(date.minusDays(2))
                .venueName(venue)
                .location(college + ", Chennai")
                .googleMapsUrl("https://maps.google.com/?q=" + college.replace(" ", "+") + "+Chennai")
                .whatsappContactNumber("9876543210")
                .whatsappGroupLink("https://chat.whatsapp.com/eventbookingdemo")
                .ticketPrice(price)
                .totalSeats(seats)
                .availableSeats(seats)
                .status(Event.EventStatus.PUBLISHED)
                .visibility(Event.EventVisibility.PUBLIC)
                .featured(featured)
                .hasCertificate(certificate)
                .foodProvided(false)
                .accommodationProvided(false)
                .tags(tags + "," + MOCK_TAG)
                .organizerDetails("Organization type: College")
                .build();
    }

    private void seedComprehensiveMockEvents(Organizer organizer) {
        String tag = "COMPREHENSIVE_MOCK_2026";
        eventRepository.deleteByTagsContaining(tag);
        String[] titles = {
                "CodeYatra National Hackathon", "React Native App Workshop", "Pragati Tech Symposium",
                "AlgoArena Coding Contest", "Bharat AI Research Conference", "BlockEdge Blockchain Summit",
                "SmartThings IoT Expo", "Cyber Kavach Security Bootcamp", "RoboNexus Challenge",
                "CloudOps Kubernetes Lab", "DataVerse Science Summit", "ML Model Sprint",
                "DevOps Pipeline Day", "Campus Startup Pitch League", "DesignSutra UI UX Jam",
                "Career Connect Engineering Fair", "FinTech AI Hack Day", "Rhythm Raaga Cultural Fest",
                "Inter College Sports Meet", "LensCraft Photography Walk",
                "Founder Forge Entrepreneurship Summit", "GitOps and SRE Workshop",
                "Women in Cyber Security Forum", "Deep Learning Masterclass",
                "GreenTech Innovation Hackathon", "Quantum Computing Colloquium",
                "Product Management Pitch Night", "No Code Automation Workshop", "Drone Robotics Expo",
                "GenAI Prompt Engineering Lab", "Sustainable Blockchain Forum", "Open Source Contribution Day"
        };
        String[] labels = {
                "Hackathon", "Workshop", "Technical Symposium", "Coding Contest", "AI Conference",
                "Blockchain Summit", "IoT Expo", "Cyber Security", "Robotics", "Cloud Computing",
                "Data Science", "Machine Learning", "DevOps", "Startup Pitch", "UI/UX", "Career Fair",
                "Hackathon", "Cultural Fest", "Sports Meet", "Photography", "Entrepreneurship", "DevOps",
                "Cyber Security", "Machine Learning", "Hackathon", "Cloud Computing", "Startup Pitch",
                "Workshop", "Robotics", "AI Conference", "Blockchain Summit", "Coding Contest"
        };
        String[] categories = {
                "HACKATHON", "WORKSHOP", "TECHNICAL_SYMPOSIUM", "CODING_COMPETITION", "SEMINAR",
                "TECHNICAL_SYMPOSIUM", "PROJECT_EXHIBITION", "SEMINAR", "TECHNICAL_SYMPOSIUM", "WORKSHOP",
                "TECHNICAL_TRAINING", "TECHNICAL_TRAINING", "TECHNICAL_SYMPOSIUM", "CLUB_ACTIVITY",
                "WORKSHOP", "PLACEMENT_PREP", "HACKATHON", "CULTURAL", "SPORTS", "CLUB_ACTIVITY",
                "WORKSHOP", "TECHNICAL_TRAINING", "SEMINAR", "TECHNICAL_TRAINING", "HACKATHON",
                "TECHNICAL_SYMPOSIUM", "CLUB_ACTIVITY", "WORKSHOP", "PROJECT_EXHIBITION", "SEMINAR",
                "TECHNICAL_SYMPOSIUM", "INTER_COLLEGE"
        };
        String[] modes = {
                "OFFLINE", "HYBRID", "OFFLINE", "ONLINE", "HYBRID", "OFFLINE", "OFFLINE", "HYBRID",
                "OFFLINE", "ONLINE", "HYBRID", "ONLINE", "OFFLINE", "OFFLINE", "HYBRID", "OFFLINE",
                "HYBRID", "OFFLINE", "OFFLINE", "OFFLINE", "HYBRID", "ONLINE", "HYBRID", "ONLINE",
                "OFFLINE", "HYBRID", "OFFLINE", "ONLINE", "OFFLINE", "ONLINE", "HYBRID", "OFFLINE"
        };
        String[] colleges = {
                "IIT Madras", "Anna University", "PSG College of Technology", "VIT Vellore",
                "SRM Institute of Science and Technology", "Amrita Vishwa Vidyapeetham",
                "Kongu Engineering College", "Coimbatore Institute of Technology", "NIT Tiruchirappalli",
                "SASTRA Deemed University", "Kumaraguru College of Technology", "Rajalakshmi Engineering College",
                "SSN College of Engineering", "Thiagarajar College of Engineering", "BMS College of Engineering",
                "PES University", "RV College of Engineering", "Christ University",
                "St. Josephs College of Engineering", "Hindustan Institute of Technology and Science",
                "Manipal Institute of Technology", "IIT Hyderabad", "Osmania University", "JNTU Hyderabad",
                "IIT Bombay", "VJTI Mumbai", "COEP Technological University", "MIT World Peace University",
                "IIT Delhi", "DTU Delhi", "Chandigarh University", "Lovely Professional University"
        };
        String[] cities = {
                "Chennai", "Chennai", "Coimbatore", "Vellore", "Chennai", "Coimbatore", "Erode",
                "Coimbatore", "Tiruchirappalli", "Thanjavur", "Coimbatore", "Chennai", "Chennai",
                "Madurai", "Bengaluru", "Bengaluru", "Bengaluru", "Bengaluru", "Chennai", "Chennai",
                "Manipal", "Hyderabad", "Hyderabad", "Hyderabad", "Mumbai", "Mumbai", "Pune", "Pune",
                "New Delhi", "New Delhi", "Chandigarh", "Jalandhar"
        };
        String[] states = {
                "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu",
                "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu", "Tamil Nadu",
                "Tamil Nadu", "Tamil Nadu", "Karnataka", "Karnataka", "Karnataka", "Karnataka",
                "Tamil Nadu", "Tamil Nadu", "Karnataka", "Telangana", "Telangana", "Telangana",
                "Maharashtra", "Maharashtra", "Maharashtra", "Maharashtra", "Delhi", "Delhi", "Punjab", "Punjab"
        };
        String[] departments = {
                "Computer Science", "Information Technology", "All Engineering Departments", "Computer Science",
                "Artificial Intelligence", "Computer Science", "Electronics and Communication", "Cyber Security",
                "Mechanical Engineering", "Cloud Computing", "Data Science", "Artificial Intelligence",
                "Information Technology", "Entrepreneurship Cell", "Design Club", "Training and Placement",
                "Computer Science", "Cultural Committee", "Physical Education", "Photography Club",
                "Entrepreneurship Cell", "Computer Science", "Cyber Security Cell", "Data Science",
                "Innovation Cell", "Physics and Computer Science", "Management Cell", "Information Technology",
                "Aeronautical Engineering", "AI Club", "Computer Science", "Open Source Club"
        };
        LocalDate[] dates = {
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 6, 18), LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 12),
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 28),
                LocalDate.of(2026, 7, 30), LocalDate.of(2026, 4, 22), LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 16), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 7, 18), LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 2, 20), LocalDate.of(2026, 6, 25),
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 1, 30), LocalDate.of(2026, 10, 18),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 15)
        };
        Event.EventStatus[] statuses = {
                Event.EventStatus.COMPLETED, Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED,
                Event.EventStatus.CANCELLED, Event.EventStatus.PUBLISHED, Event.EventStatus.PUBLISHED,
                Event.EventStatus.PUBLISHED, Event.EventStatus.PENDING_APPROVAL, Event.EventStatus.PUBLISHED,
                Event.EventStatus.PUBLISHED, Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED,
                Event.EventStatus.PUBLISHED, Event.EventStatus.PENDING_APPROVAL, Event.EventStatus.DRAFT,
                Event.EventStatus.PUBLISHED, Event.EventStatus.PUBLISHED, Event.EventStatus.COMPLETED,
                Event.EventStatus.PUBLISHED, Event.EventStatus.CANCELLED, Event.EventStatus.PUBLISHED,
                Event.EventStatus.PENDING_APPROVAL, Event.EventStatus.COMPLETED, Event.EventStatus.EXPIRED,
                Event.EventStatus.PUBLISHED, Event.EventStatus.PUBLISHED, Event.EventStatus.DRAFT,
                Event.EventStatus.PUBLISHED, Event.EventStatus.COMPLETED, Event.EventStatus.PUBLISHED,
                Event.EventStatus.PENDING_APPROVAL, Event.EventStatus.PUBLISHED
        };
        int[] prices = {
                199, 99, 299, 99, 499, 299, 0, 199, 299, 199, 299, 499, 299, 0, 99, 0,
                999, 0, 0, 99, 299, 199, 0, 499, 299, 499, 199, 99, 299, 199, 499, 0
        };
        int[] capacities = {
                250, 100, 500, 150, 500, 250, 150, 100, 100, 150, 250, 100, 150, 100, 100, 500,
                250, 500, 500, 50, 250, 100, 250, 150, 250, 500, 100, 150, 150, 250, 250, 500
        };

        for (int i = 0; i < titles.length; i++) {
            eventRepository.save(comprehensiveEvent(organizer, i, titles[i], labels[i], categories[i], modes[i],
                    colleges[i], cities[i], states[i], departments[i], dates[i], statuses[i],
                    prices[i], capacities[i], tag));
        }
    }

    private Event comprehensiveEvent(Organizer organizer, int index, String title, String label, String category,
                                     String mode, String college, String city, String state, String department,
                                     LocalDate date, Event.EventStatus status, int price, int capacity, String tag) {
        int n = index + 1;
        int booked = status == Event.EventStatus.CANCELLED ? 0 :
                Math.min(capacity - 5, (int) (capacity * (0.25 + ((index % 6) * 0.08))));
        if (status == Event.EventStatus.COMPLETED) booked = (int) (capacity * 0.82);
        int available = Math.max(0, capacity - booked);
        String phone = "91987066" + String.format("%04d", n);
        String pinCode = "600" + String.format("%03d", n);
        String block = "Block " + (char) ('A' + (index % 5));
        String hall = "Hall " + (100 + n);
        String venue = List.of("Main Auditorium", "Innovation Lab", "Seminar Hall A", "Virtual Arena",
                "Convention Centre", "Incubation Centre", "Project Gallery", "Cyber Lab",
                "Robotics Arena", "Cloud Lab").get(index % 10);
        String accommodationType = List.of("HOSTEL", "HOTEL", "BOTH", "NONE").get(index % 4);
        boolean accommodation = !"NONE".equals(accommodationType);
        int accommodationCost = accommodation ? 500 + ((index % 5) * 250) : 0;
        String regStart = date.minusDays(30).toString();
        String regEnd = date.equals(LocalDate.of(2026, 7, 12)) ? date.toString() : date.minusDays(2).toString();
        String displayStatus = status == Event.EventStatus.EXPIRED ? "Registration Closed" : status.name().replace("_", " ");
        String organizerDetails = "Organizer Name: " + List.of("Arjun Nair", "Priya Menon", "Sanjay Rao", "Kavya Iyer").get(index % 4)
                + "; Organization Name: " + college + " " + department
                + "; Organizer Email: event" + String.format("%02d", n) + "@eventgpt-campus.example.com"
                + "; Organizer Phone: " + phone
                + "; WhatsApp Number: " + phone
                + "; Website: https://events" + n + ".example.edu"
                + "; Instagram: https://instagram.com/eventgpt" + n
                + "; LinkedIn: https://linkedin.com/company/eventgpt" + n
                + "; X: https://x.com/eventgpt" + n
                + "; Facebook: https://facebook.com/eventgpt" + n
                + "; Contact Person: " + List.of("Neha Kapoor", "Aditya Kumar", "Sneha Thomas", "Rahul Jain").get(index % 4)
                + "; Contact Mobile: " + phone
                + "; Contact Email: contact" + n + "@eventgpt-campus.example.com"
                + "; Eligibility: " + List.of("Any College Student", "Engineering Students", "Final Year Only", "Open to Everyone").get(index % 4)
                + "; Prizes: " + List.of("Rs 10000", "Rs 50000", "Rs 100000", "Goodies + Internship Opportunities").get(index % 4)
                + "; Sponsors: TCS iON, Zoho, Freshworks, Razorpay"
                + "; Rating: " + String.format("%.1f", 4.1 + ((index % 10) * 0.09))
                + "; Review Count: " + (35 + index * 7)
                + "; Registration Count: " + booked
                + "; Bookmark Count: " + (12 + index * 5)
                + "; Share Count: " + (20 + index * 6);

        return Event.builder()
                .organizer(organizer)
                .eventName(title)
                .description(label + " covering practical sessions, mentoring, networking, judging, certificates, "
                        + "help desk support and post-event resources. Full Description: " + title
                        + " brings together students, faculty and industry mentors for realistic hands-on learning.")
                .category(category)
                .eventType(mode)
                .collegeName(college)
                .departmentName(department)
                .eventBanner(relatedPosterUrl(label, n))
                .eventImages(relatedGalleryUrl(label, n))
                .authorizedDocumentUrl("https://example.com/documents/eventgpt2026-" + n + "-approval.pdf")
                .eventDate(date)
                .eventTime(LocalTime.of(9, 0))
                .endDate(date)
                .endTime(LocalTime.of(17, 30))
                .registrationDeadline(LocalDate.parse(regEnd))
                .venueName(venue)
                .location(college + ", " + block + ", " + hall + ", " + city + ", " + state + ", India - " + pinCode)
                .googleMapsUrl("https://maps.google.com/?q=" + college.replace(" ", "+") + "+" + city)
                .whatsappContactNumber(phone)
                .whatsappGroupLink("https://chat.whatsapp.com/eventgpt2026seed" + String.format("%02d", n))
                .ticketPrice(new BigDecimal(price + ".00"))
                .totalSeats(capacity)
                .availableSeats(available)
                .status(status)
                .visibility(Event.EventVisibility.PUBLIC)
                .featured(index % 3 == 0)
                .hasCertificate(true)
                .foodProvided(true)
                .foodMeals(List.of("BREAKFAST,LUNCH,SNACKS,DINNER", "LUNCH,SNACKS", "BREAKFAST,LUNCH", "SNACKS").get(index % 4))
                .foodType(List.of("VEG", "BOTH", "VEG", "NON_VEG").get(index % 4))
                .teaCoffeeProvided(true)
                .specialDiet("Jain food and allergy-friendly meals on prior request")
                .accommodationProvided(accommodation)
                .accommodationType(accommodationType)
                .accommodationCharges(accommodationCost)
                .accommodationBedsAvailable(accommodation ? 30 + ((index % 6) * 10) : 0)
                .accommodationDetails((accommodation ? "Hostel or partner hotel available" : "No accommodation")
                        + "; Cost: Rs " + accommodationCost + "; Distance from venue: "
                        + String.format("%.1f", 0.5 + ((index % 6) * 0.4))
                        + " km; Booking instructions: email accommodation" + n
                        + "@eventgpt-campus.example.com with ticket ID before " + regEnd + ".")
                .boysHostelAvailable("HOSTEL".equals(accommodationType) || "BOTH".equals(accommodationType))
                .girlsHostelAvailable("HOSTEL".equals(accommodationType) || "BOTH".equals(accommodationType))
                .hotelTieupAvailable("HOTEL".equals(accommodationType) || "BOTH".equals(accommodationType))
                .accommodationCheckIn("Previous day 18:00")
                .accommodationCheckOut("Event day 20:00")
                .accommodationContactPerson("Accommodation Desk " + n)
                .nearestBusStop(city + " Central Bus Stand")
                .distanceFromBusStop((1 + (index % 6)) + " km")
                .busNumbers("5A, 7C, S12")
                .nearestRailwayStation(city + " Junction")
                .distanceFromRailwayStation((3 + (index % 8)) + " km")
                .nearestAirport(city + " International Airport")
                .metroInformation(List.of("Chennai", "Bengaluru", "Hyderabad", "Mumbai", "New Delhi").contains(city)
                        ? "Available on city metro corridor" : "Not Available")
                .landmarks("Main Gate, Admin Block, Library Circle")
                .parkingAvailable(List.of("FREE", "LIMITED", "PAID", "FREE").get(index % 4))
                .travelGuide("If you arrive at " + city + " Central Bus Stand, board bus 5A towards " + college
                        + ". Get down at the college main gate, walk approximately 300 meters to " + venue
                        + ". Railway passengers can take an auto from " + city
                        + " Junction. Airport cabs are available at prepaid counters. Parking is available near " + block + ".")
                .estimatedTravelTime("20-45 minutes from major transit points")
                .cabEstimate("Rs " + (180 + ((index % 6) * 70)) + "-Rs " + (450 + ((index % 6) * 90)) + " by app cab")
                .nearbyHotels("Accommodation: " + accommodationType + "; Hotel partnership: "
                        + ("HOTEL".equals(accommodationType) || "BOTH".equals(accommodationType))
                        + "; Guest house: " + (index % 7 == 0))
                .nearbyRestaurants("Campus canteen, student cafe, nearby vegetarian hotel within 1 km")
                .emergencyContacts("Campus security: " + phone + "; Medical desk: 91988000" + String.format("%04d", n))
                .sessionSchedule("09:00 Registration | 10:00 Inauguration | 11:00 Keynote | 12:30 Lunch | "
                        + "14:00 Technical Session | 16:00 Awards | 17:00 Closing")
                .speakerList("Industry mentors, alumni speakers, faculty coordinators")
                .liveAnnouncements("Registration Start Date: " + regStart + "; Registration End Date: " + regEnd
                        + "; Display Status: " + displayStatus
                        + "; Certificate Types: Participation, Winner, Runner-up, Volunteer"
                        + "; Certificate Template Status: Uploaded"
                        + "; FAQs: Is ID card mandatory? Yes. Can other colleges join? Based on eligibility. "
                        + "Are certificates provided? Yes. Is food included? Yes. Can I get a refund? See refund policy.")
                .reportingTime(LocalTime.of(8, 30))
                .dressCode("Smart casuals or college T-shirt")
                .itemsToBring("College ID, laptop/charger when applicable, water bottle, notebook")
                .laptopRequired(List.of("HACKATHON", "WORKSHOP", "TECHNICAL_TRAINING", "CODING_COMPETITION").contains(category))
                .idCardRequired(true)
                .teamSize(List.of("Solo", "1-2 members", "2-4 members", "3-5 members").get(index % 4))
                .rules("Follow college discipline, original work only, judges decision final, no plagiarism or abusive conduct.")
                .refundPolicy(price == 0
                        ? "Free event; no payment refund applies. Inform organizer before registration closes to release seat."
                        : "90% refund until 7 days before event, 50% refund until 48 hours before event, no refund after check-in.")
                .cancellationPolicy("Organizer may cancel or reschedule due to low registrations, safety issues, speaker unavailability, or force majeure. Paid participants receive refund or transfer options within 7 working days.")
                .certificateEligibility("Minimum 75% attendance and valid registration required. Winner and runner-up certificates issued after result verification.")
                .wifiAvailable(true)
                .wheelchairAccessible(false)
                .restRoomsAvailable(false)
                .drinkingWaterAvailable(false)
                .medicalSupportAvailable(true)
                .networkingEnabled(false)
                .tags(tag + "," + label + "," + category)
                .organizerDetails(organizerDetails)
                .build();
    }

    private String relatedPosterUrl(String label, int seed) {
        return "/event-posters/" + posterSlug(label) + ".svg";
    }

    private String relatedGalleryUrl(String label, int seed) {
        return "/event-posters/" + posterSlug(label) + ".svg";
    }

    private String posterSlug(String label) {
        return switch (label) {
            case "Hackathon" -> "hackathon";
            case "Workshop" -> "workshop";
            case "Technical Symposium" -> "technical-symposium";
            case "Coding Contest" -> "coding-contest";
            case "AI Conference" -> "ai-conference";
            case "Blockchain Summit" -> "blockchain-summit";
            case "IoT Expo" -> "iot-expo";
            case "Cyber Security" -> "cyber-security";
            case "Robotics" -> "robotics";
            case "Cloud Computing" -> "cloud-computing";
            case "Startup Pitch" -> "startup-pitch";
            case "UI/UX" -> "ui-ux";
            case "Data Science" -> "data-science";
            case "Machine Learning" -> "machine-learning";
            case "DevOps" -> "devops";
            case "Career Fair" -> "career-fair";
            case "Cultural Fest" -> "cultural-fest";
            case "Sports Meet" -> "sports-meet";
            case "Photography" -> "photography";
            case "Entrepreneurship" -> "entrepreneurship";
            default -> "college-event";
        };
    }

    private void refreshMockPosterOverrides() {
        eventRepository.findAll().stream()
                .filter(event -> event.getTags() != null
                        && (event.getTags().contains(MOCK_TAG)
                        || event.getTags().contains("COMPREHENSIVE_MOCK_2026")))
                .forEach(event -> {
                    if ("HACKATHON".equals(event.getCategory())) {
                        event.setEventBanner("/event-posters/hackathon.svg");
                        event.setEventImages("/event-posters/hackathon.svg");
                    } else if ("CODING_COMPETITION".equals(event.getCategory())) {
                        event.setEventBanner("/event-posters/coding-contest.svg");
                        event.setEventImages("/event-posters/coding-contest.svg");
                    }
                    eventRepository.save(event);
                });
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
