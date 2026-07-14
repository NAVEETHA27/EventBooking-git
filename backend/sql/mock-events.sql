USE event_booking_db;

INSERT INTO organizers (
    organizer_code,
    organizer_name,
    organization_name,
    email,
    password_hash,
    phone,
    city,
    state,
    country,
    email_verified,
    is_approved,
    role,
    created_at,
    updated_at
)
SELECT
    'ORGMOCK',
    'Campus Events Office',
    'NovaTech Institute',
    'mock.organizer@collegeevents.local',
    '$2a$10$mockseedpasswordhashforlocaldataonly',
    '9876543210',
    'Chennai',
    'Tamil Nadu',
    'India',
    TRUE,
    TRUE,
    'ORGANIZER',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM organizers WHERE email = 'mock.organizer@collegeevents.local'
);

UPDATE organizers
SET organizer_code = 'ORGMOCK'
WHERE email = 'mock.organizer@collegeevents.local'
  AND (organizer_code IS NULL OR organizer_code = '');

DELETE FROM events
WHERE tags LIKE '%MOCK_SEED_2026%';

INSERT INTO events (
    organizer_id,
    event_name,
    description,
    category,
    event_type,
    event_banner,
    event_date,
    event_time,
    end_date,
    end_time,
    venue_name,
    location,
    google_maps_url,
    ticket_price,
    total_seats,
    available_seats,
    status,
    visibility,
    is_featured,
    college_name,
    department_name,
    has_certificate,
    registration_deadline,
    tags,
    organizer_details,
    authorized_document_url,
    created_at,
    updated_at
)
VALUES
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'AI Hackathon 2026', 'Build AI-powered campus solutions in a 24-hour team challenge with mentor support and final demos.', 'HACKATHON', 'OFFLINE', '/uploads/banners/mock-ai-hackathon.jpg', '2026-07-18', '09:00:00', '2026-07-19', '17:00:00', 'Innovation Lab', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=NovaTech+Institute+Chennai', 250.00, 120, 120, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Computer Science', TRUE, '2026-07-15', 'AI,Hackathon,MOCK_SEED_2026', 'Organization type: College', '/uploads/authorized-documents/mock-ai-hackathon-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Cloud Native Workshop', 'Hands-on Docker, Kubernetes, and deployment workflow workshop for students preparing for modern backend roles.', 'WORKSHOP', 'OFFLINE', '/uploads/banners/mock-cloud-workshop.jpg', '2026-07-22', '10:00:00', '2026-07-22', '16:00:00', 'Seminar Hall A', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Seminar+Hall+Chennai', 150.00, 80, 80, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Information Technology', TRUE, '2026-07-20', 'Cloud,Kubernetes,Workshop,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-cloud-workshop-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Cyber Shield Seminar', 'A focused seminar on phishing defense, secure coding basics, password hygiene, and career paths in cybersecurity.', 'SEMINAR', 'HYBRID', '/uploads/banners/mock-cyber-seminar.jpg', '2026-07-25', '14:00:00', '2026-07-25', '16:30:00', 'Auditorium', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Auditorium+Chennai', 0.00, 250, 250, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Cyber Security', TRUE, '2026-07-23', 'Cybersecurity,Seminar,MOCK_SEED_2026', 'Organization type: College', '/uploads/authorized-documents/mock-cyber-seminar-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Robo Race Challenge', 'Design, tune, and race autonomous robots through a timed obstacle track.', 'TECHNICAL_SYMPOSIUM', 'OFFLINE', '/uploads/banners/mock-robo-race.jpg', '2026-08-02', '09:30:00', '2026-08-02', '15:30:00', 'Mechanical Workshop Arena', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Mechanical+Workshop+Chennai', 200.00, 100, 100, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Mechanical Engineering', TRUE, '2026-07-30', 'Robotics,Technical,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-robo-race-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Cultural Night Auditions', 'Open auditions for music, dance, theatre, and stand-up performances for the annual cultural night.', 'CULTURAL', 'OFFLINE', '/uploads/banners/mock-cultural-night.jpg', '2026-08-05', '16:00:00', '2026-08-05', '20:00:00', 'Open Air Theatre', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Open+Air+Theatre+Chennai', 0.00, 300, 300, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Cultural Club', FALSE, '2026-08-03', 'Cultural,Auditions,MOCK_SEED_2026', 'Organization type: Club', '/uploads/authorized-documents/mock-cultural-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Full Stack Sprint', 'A practical coding competition covering REST APIs, React UI flows, database design, and deployment basics.', 'CODING_COMPETITION', 'OFFLINE', '/uploads/banners/mock-fullstack-sprint.jpg', '2026-08-09', '09:00:00', '2026-08-09', '18:00:00', 'Computer Lab 3', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Computer+Lab+Chennai', 100.00, 90, 90, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Computer Science', TRUE, '2026-08-06', 'Fullstack,Coding,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-fullstack-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Startup Pitch Day', 'Student teams pitch startup ideas to mentors and alumni founders for feedback, networking, and incubation support.', 'CLUB_ACTIVITY', 'OFFLINE', '/uploads/banners/mock-startup-pitch.jpg', '2026-08-14', '13:00:00', '2026-08-14', '17:00:00', 'Incubation Centre', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Incubation+Centre+Chennai', 0.00, 110, 110, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Entrepreneurship Cell', TRUE, '2026-08-12', 'Startup,Pitch,ECell,MOCK_SEED_2026', 'Organization type: Club', '/uploads/authorized-documents/mock-startup-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Placement Aptitude Bootcamp', 'A full-day bootcamp on quantitative aptitude, logical reasoning, resume hygiene, and interview preparation.', 'PLACEMENT_PREP', 'OFFLINE', '/uploads/banners/mock-placement-bootcamp.jpg', '2026-08-20', '09:30:00', '2026-08-20', '16:30:00', 'Training Hall', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Training+Hall+Chennai', 75.00, 180, 180, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Training and Placement', TRUE, '2026-08-18', 'Placement,Aptitude,MOCK_SEED_2026', 'Organization type: College', '/uploads/authorized-documents/mock-placement-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'IoT Project Expo', 'Showcase sensor, automation, and embedded systems projects built by student teams.', 'PROJECT_EXHIBITION', 'OFFLINE', '/uploads/banners/mock-iot-expo.jpg', '2026-08-27', '10:00:00', '2026-08-27', '15:00:00', 'ECE Project Gallery', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=ECE+Project+Gallery+Chennai', 0.00, 160, 160, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Electronics and Communication', TRUE, '2026-08-24', 'IoT,Expo,Projects,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-iot-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Data Analytics Technical Training', 'Learn spreadsheet analytics, SQL querying, dashboarding, and storytelling with campus datasets.', 'TECHNICAL_TRAINING', 'HYBRID', '/uploads/banners/mock-data-training.jpg', '2026-09-03', '10:00:00', '2026-09-03', '17:00:00', 'Analytics Lab', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Analytics+Lab+Chennai', 180.00, 70, 70, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Data Science', TRUE, '2026-09-01', 'Data,Analytics,Training,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-data-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Inter College Quiz League', 'Compete with teams from nearby colleges in technology, current affairs, science, and campus trivia rounds.', 'INTER_COLLEGE', 'OFFLINE', '/uploads/banners/mock-quiz-league.jpg', '2026-09-10', '11:00:00', '2026-09-10', '15:00:00', 'Main Auditorium', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Main+Auditorium+Chennai', 50.00, 220, 220, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Student Affairs', TRUE, '2026-09-07', 'Quiz,InterCollege,MOCK_SEED_2026', 'Organization type: College', '/uploads/authorized-documents/mock-quiz-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Intra College Sports Meet', 'A campus sports meet with track events, volleyball, throwball, chess, and department-level team points.', 'INTRA_COLLEGE', 'OFFLINE', '/uploads/banners/mock-sports-meet.jpg', '2026-09-18', '08:00:00', '2026-09-18', '18:00:00', 'College Sports Ground', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Sports+Ground+Chennai', 0.00, 500, 500, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Physical Education', FALSE, '2026-09-15', 'Sports,IntraCollege,MOCK_SEED_2026', 'Organization type: College', '/uploads/authorized-documents/mock-sports-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Women in Tech Panel', 'A panel discussion with alumni and industry mentors on technical careers, leadership, and community support.', 'SEMINAR', 'OFFLINE', '/uploads/banners/mock-women-tech.jpg', '2026-09-24', '15:00:00', '2026-09-24', '17:00:00', 'Mini Auditorium', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Mini+Auditorium+Chennai', 0.00, 180, 180, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Women in Engineering Cell', TRUE, '2026-09-22', 'WomenInTech,Seminar,MOCK_SEED_2026', 'Organization type: Club', '/uploads/authorized-documents/mock-wit-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'Green Campus Innovation Workshop', 'Brainstorm and prototype sustainability ideas for energy savings, waste reduction, and greener campus operations.', 'WORKSHOP', 'OFFLINE', '/uploads/banners/mock-green-campus.jpg', '2026-10-01', '10:00:00', '2026-10-01', '16:00:00', 'Civil Seminar Room', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Civil+Seminar+Room+Chennai', 60.00, 75, 75, 'PUBLISHED', 'PUBLIC', FALSE, 'NovaTech Institute', 'Civil Engineering', TRUE, '2026-09-29', 'Sustainability,Workshop,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-green-approval.pdf', NOW(), NOW()),
((SELECT id FROM organizers WHERE email = 'mock.organizer@collegeevents.local'), 'AR/VR Experience Day', 'Explore immersive demos and mini-builds using augmented and virtual reality tools for education and entertainment.', 'TECHNICAL_SYMPOSIUM', 'HYBRID', '/uploads/banners/mock-arvr-day.jpg', '2026-10-08', '11:00:00', '2026-10-08', '17:00:00', 'Media Lab', 'NovaTech Institute, Chennai', 'https://maps.google.com/?q=Media+Lab+Chennai', 120.00, 95, 95, 'PUBLISHED', 'PUBLIC', TRUE, 'NovaTech Institute', 'Information Technology', TRUE, '2026-10-05', 'AR,VR,Technical,MOCK_SEED_2026', 'Organization type: Department', '/uploads/authorized-documents/mock-arvr-approval.pdf', NOW(), NOW());

UPDATE events
SET whatsapp_contact_number = '9876543210',
    whatsapp_group_link = 'https://chat.whatsapp.com/eventbookingdemo'
WHERE tags LIKE '%MOCK_SEED_2026%';
