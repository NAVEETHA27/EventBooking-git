-- ════════════════════════════════════════════════════════════════════
-- MOCK DATA — College Event Booking System
-- Run AFTER schema.sql. Uses bcrypt hash of "Password@123" for all accounts.
-- ════════════════════════════════════════════════════════════════════
USE event_booking_db;

-- ── ORGANIZERS (10) ──────────────────────────────────────────────────
INSERT IGNORE INTO organizers (id, organizer_code, organizer_name, organization_name, email, password_hash, phone, city, state, country, email_verified, is_approved, role, created_at, updated_at) VALUES
(1,'ORG0001','Tech Events Team','NovaTech Institute','techevents@novatech.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919876543210','Chennai','Tamil Nadu','India',1,1,'ORGANIZER',NOW(),NOW()),
(2,'ORG0002','Cultural Club','Sri Venkateswara College','cultural@svcollege.ac.in','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919765432109','Tirupati','Andhra Pradesh','India',1,1,'ORGANIZER',NOW(),NOW()),
(3,'ORG0003','CSE Department','PSG College of Technology','cse@psgtech.ac.in','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919654321098','Coimbatore','Tamil Nadu','India',1,1,'ORGANIZER',NOW(),NOW()),
(4,'ORG0004','Sports Council','Anna University','sports@annauniv.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919543210987','Chennai','Tamil Nadu','India',1,1,'ORGANIZER',NOW(),NOW()),
(5,'ORG0005','IEEE Student Branch','VIT University','ieee@vit.ac.in','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919432109876','Vellore','Tamil Nadu','India',1,1,'ORGANIZER',NOW(),NOW());

-- ── USERS (20) ───────────────────────────────────────────────────────
INSERT IGNORE INTO users (id, user_code, name, email, password_hash, phone, organization_name, city, email_verified, role, created_at, updated_at) VALUES
(1,'USR0001','Arun Kumar','arun@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919111111111','PSG College of Technology','Coimbatore',1,'USER',NOW(),NOW()),
(2,'USR0002','Priya Sharma','priya@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919222222222','Anna University','Chennai',1,'USER',NOW(),NOW()),
(3,'USR0003','Rahul Verma','rahul@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919333333333','VIT University','Vellore',1,'USER',NOW(),NOW()),
(4,'USR0004','Sneha Patel','sneha@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919444444444','NovaTech Institute','Chennai',1,'USER',NOW(),NOW()),
(5,'USR0005','Karthik Raja','karthik@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919555555555','PSG College of Technology','Coimbatore',1,'USER',NOW(),NOW()),
(6,'USR0006','Anjali Singh','anjali@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919666666666','NIT Trichy','Tiruchirappalli',1,'USER',NOW(),NOW()),
(7,'USR0007','Vikram Nair','vikram@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919777777777','SRM Institute','Chennai',1,'USER',NOW(),NOW()),
(8,'USR0008','Divya Lakshmi','divya@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919888888888','Amrita University','Coimbatore',1,'USER',NOW(),NOW()),
(9,'USR0009','Suresh Babu','suresh@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919999999991','Kongu Engineering','Erode',1,'USER',NOW(),NOW()),
(10,'USR0010','Meena Iyer','meena@student.edu','$2a$12$8Kn5sA9vQwBxLmP3rT7uXeH6cD4fG2jN1qW8oE0pR5tY3uI6vK9mZ','+919999999992','CEG Anna University','Chennai',1,'USER',NOW(),NOW());

-- ── USER INTERESTS ────────────────────────────────────────────────────
INSERT IGNORE INTO user_interests (user_id, skills, interests, department, college, year_of_study, favorite_categories, preferred_event_type, career_goal, updated_at) VALUES
(1,'Java,Spring Boot,React','AI,Cloud','Computer Science','PSG College of Technology','3rd Year','HACKATHON,WORKSHOP','OFFLINE','Software Engineer',NOW()),
(2,'Python,ML,Data Science','AI,Data','Information Technology','Anna University','2nd Year','WORKSHOP,SEMINAR','ONLINE','Data Scientist',NOW()),
(3,'C++,DSA,Competitive Coding','Sports,Coding','CSE','VIT University','4th Year','HACKATHON,CODING_COMPETITION','OFFLINE','Product Manager',NOW()),
(4,'UI/UX,Figma,React','Design,Frontend','IT','NovaTech Institute','2nd Year','WORKSHOP,CULTURAL','HYBRID','UX Designer',NOW()),
(5,'Java,Spring,MySQL','Backend,Cloud','CSE','PSG College of Technology','3rd Year','HACKATHON,TECHNICAL_SYMPOSIUM','OFFLINE','Backend Engineer',NOW());

-- ── EVENTS (15 varied) ───────────────────────────────────────────────
INSERT IGNORE INTO events (id,organizer_id,event_name,description,category,event_type,college_name,department_name,event_date,event_time,end_date,total_seats,available_seats,ticket_price,status,visibility,has_certificate,food_provided,accommodation_provided,location,venue_name,google_maps_url,whatsapp_contact_number,tags,created_at,updated_at,version) VALUES
(1,3,'CodeStorm Hackathon 2026','48-hour national hackathon with ₹1L prize pool. Build innovative solutions using AI and Cloud.','HACKATHON','OFFLINE','PSG College of Technology','CSE','2026-08-10','09:00','2026-08-11',200,120,499,'PUBLISHED','PUBLIC',1,1,1,'Coimbatore, Tamil Nadu','PSG Tech Auditorium','https://maps.google.com','9876543210','hackathon,AI,cloud,prize,team',NOW(),NOW(),0),
(2,1,'AI/ML Workshop Series','Hands-on 2-day workshop on Machine Learning with Python. Certificate provided.','WORKSHOP','OFFLINE','NovaTech Institute','CSE','2026-08-15','10:00','2026-08-16',100,42,299,'PUBLISHED','PUBLIC',1,1,0,'Chennai, Tamil Nadu','NovaTech Seminar Hall','https://maps.google.com','9111111111','workshop,AI,ML,Python,certificate',NOW(),NOW(),0),
(3,5,'IEEE TechFest 2026','Annual technical symposium with coding contests, paper presentations and robotics.','TECHNICAL_SYMPOSIUM','OFFLINE','VIT University','ECE','2026-08-20','09:00',NULL,500,280,199,'PUBLISHED','PUBLIC',1,1,1,'Vellore, Tamil Nadu','VIT MB Block','https://maps.google.com','9432109876','symposium,IEEE,robotics,coding',NOW(),NOW(),0),
(4,2,'Inter-College Cultural Fest','Music, dance, drama and art competitions open to all colleges.','CULTURAL','OFFLINE','Sri Venkateswara College',NULL,'2026-08-25','10:00','2026-08-26',1000,600,99,'PUBLISHED','PUBLIC',0,1,1,'Tirupati, Andhra Pradesh','SVC Open Auditorium','https://maps.google.com','9765432109','cultural,dance,music,drama',NOW(),NOW(),0),
(5,3,'Web Dev Bootcamp','Full-stack web development with React, Node.js and MongoDB. 3-day intensive.','WORKSHOP','OFFLINE','PSG College of Technology','IT','2026-09-01','09:00','2026-09-03',60,15,799,'PUBLISHED','PUBLIC',1,1,0,'Coimbatore, Tamil Nadu','PSG IT Lab','https://maps.google.com','9654321098','react,nodejs,mongodb,fullstack',NOW(),NOW(),0);

INSERT IGNORE INTO events (id,organizer_id,event_name,description,category,event_type,college_name,department_name,event_date,event_time,total_seats,available_seats,ticket_price,status,visibility,has_certificate,food_provided,accommodation_provided,location,venue_name,tags,created_at,updated_at,version) VALUES
(6,4,'Sports Carnival 2026','Cricket, football, volleyball and athletics competitions.','SPORTS','OFFLINE','Anna University',NULL,'2026-09-05','08:00',800,550,49,'PUBLISHED','PUBLIC',0,1,0,'Chennai, Tamil Nadu','Anna University Grounds','sports,cricket,football,athletics',NOW(),NOW(),0),
(7,1,'Cloud Computing Seminar','Introduction to AWS, Azure and GCP. Industry experts as speakers.','SEMINAR','ONLINE',NULL,NULL,'2026-09-10','11:00',500,380,0,'PUBLISHED','PUBLIC',1,0,0,'Online','Google Meet','cloud,AWS,Azure,GCP,free',NOW(),NOW(),0),
(8,5,'Cybersecurity CTF','Capture the Flag competition for ethical hackers. Teams of 3.','CODING_COMPETITION','ONLINE',NULL,NULL,'2026-09-12','10:00',200,140,149,'PUBLISHED','PUBLIC',1,0,0,'Online','HackTheBox Platform','CTF,cybersecurity,hacking,competition',NOW(),NOW(),0),
(9,2,'Classical Dance Workshop','Bharatanatyam and folk dance workshop by renowned artists.','CULTURAL','OFFLINE','Sri Venkateswara College',NULL,'2026-09-15','14:00',80,60,199,'PUBLISHED','PUBLIC',1,0,0,'Tirupati, Andhra Pradesh','SVC Dance Studio','dance,classical,bharatanatyam',NOW(),NOW(),0),
(10,3,'Data Structures Masterclass','Advanced DSA and competitive programming techniques.','WORKSHOP','OFFLINE','PSG College of Technology','CSE','2026-09-18','09:00',120,95,299,'PUBLISHED','PUBLIC',1,1,0,'Coimbatore, Tamil Nadu','PSG Main Block','DSA,algorithms,competitive,coding',NOW(),NOW(),0),
(11,1,'Startup Pitch Day','Present your startup idea to industry investors and mentors.','SEMINAR','OFFLINE','NovaTech Institute',NULL,'2026-07-15','10:00','2026-07-15',150,0,99,'COMPLETED','PUBLIC',1,1,0,'Chennai, Tamil Nadu','NovaTech Auditorium','startup,pitch,entrepreneurship,investors',NOW(),NOW(),0),
(12,5,'IoT Workshop','Internet of Things with Raspberry Pi and Arduino. Bring your laptop.','WORKSHOP','OFFLINE','VIT University','ECE','2026-07-20','09:00','2026-07-20',60,0,399,'COMPLETED','PUBLIC',1,1,0,'Vellore, Tamil Nadu','VIT IoT Lab','IoT,arduino,raspberry,embedded',NOW(),NOW(),0),
(13,3,'Free Coding Bootcamp','Learn Python from scratch. Completely free for beginners.','WORKSHOP','ONLINE',NULL,NULL,'2026-10-01','10:00',300,240,0,'PUBLISHED','PUBLIC',1,0,0,'Online','Zoom','python,free,beginners,coding',NOW(),NOW(),0),
(14,1,'National Hackathon Finals','Top 50 teams competing for the national championship.','HACKATHON','OFFLINE','NovaTech Institute','CSE','2026-10-10','09:00','2026-10-12',200,180,999,'PUBLISHED','PUBLIC',1,1,1,'Chennai, Tamil Nadu','NovaTech Convention Centre','hackathon,national,championship,prize',NOW(),NOW(),0),
(15,4,'Annual Sports Meet','Inter-department sports meet with 15 sports categories.','SPORTS','OFFLINE','Anna University',NULL,'2026-10-20','08:00','2026-10-21',600,420,29,'PUBLISHED','PUBLIC',0,1,0,'Chennai, Tamil Nadu','Anna University Stadium','sports,annual,inter-department',NOW(),NOW(),0);

-- ── BOOKINGS ─────────────────────────────────────────────────────────
INSERT IGNORE INTO bookings (id,ticket_id,user_id,event_id,quantity,total_amount,booking_status,ticket_status,booked_at,updated_at) VALUES
(1,'TKT-ARUN-001',1,1,2,998,'CONFIRMED','ACTIVE',NOW(),NOW()),
(2,'TKT-PRIYA-001',2,2,1,299,'CONFIRMED','ACTIVE',NOW(),NOW()),
(3,'TKT-RAHUL-001',3,3,1,199,'CONFIRMED','ACTIVE',NOW(),NOW()),
(4,'TKT-SNEHA-001',4,4,2,198,'CONFIRMED','ACTIVE',NOW(),NOW()),
(5,'TKT-KART-001',5,5,1,799,'CONFIRMED','ACTIVE',NOW(),NOW()),
(6,'TKT-ARUN-002',1,11,1,99,'CONFIRMED','ACTIVE','2026-07-10',NOW()),
(7,'TKT-PRIYA-002',2,11,1,99,'CONFIRMED','ACTIVE','2026-07-10',NOW()),
(8,'TKT-RAHUL-002',3,12,1,399,'CONFIRMED','ACTIVE','2026-07-15',NOW()),
(9,'TKT-ANJALI-001',6,1,1,499,'CONFIRMED','ACTIVE',NOW(),NOW()),
(10,'TKT-VIKRAM-001',7,7,1,0,'CONFIRMED','ACTIVE',NOW(),NOW());

-- ── PARTICIPANTS ──────────────────────────────────────────────────────
INSERT IGNORE INTO participants (booking_id,event_id,name,email,department,college,created_at) VALUES
(1,1,'Arun Kumar','arun@student.edu','CSE','PSG College of Technology',NOW()),
(2,2,'Priya Sharma','priya@student.edu','IT','Anna University',NOW()),
(3,3,'Rahul Verma','rahul@student.edu','CSE','VIT University',NOW()),
(6,11,'Arun Kumar','arun@student.edu','CSE','PSG College of Technology','2026-07-10'),
(7,11,'Priya Sharma','priya@student.edu','IT','Anna University','2026-07-10'),
(8,12,'Rahul Verma','rahul@student.edu','CSE','VIT University','2026-07-15');

-- ── PAYMENTS ─────────────────────────────────────────────────────────
INSERT IGNORE INTO payments (id,transaction_id,booking_id,amount,payment_status,payment_method,gateway_reference,paid_at) VALUES
(1,'TXN-001-ARUN',1,998,'SUCCESSFUL','RAZORPAY','GW-ARUN001',NOW()),
(2,'TXN-002-PRIYA',2,299,'SUCCESSFUL','RAZORPAY','GW-PRIYA001',NOW()),
(3,'TXN-003-RAHUL',3,199,'SUCCESSFUL','RAZORPAY','GW-RAHUL001',NOW()),
(4,'TXN-004-SNEHA',4,198,'SUCCESSFUL','RAZORPAY','GW-SNEHA001',NOW()),
(5,'TXN-005-KART',5,799,'SUCCESSFUL','RAZORPAY','GW-KART001',NOW()),
(6,'TXN-006-ARUN2',6,99,'SUCCESSFUL','RAZORPAY','GW-ARUN002','2026-07-10'),
(7,'TXN-007-PRIYA2',7,99,'SUCCESSFUL','RAZORPAY','GW-PRIYA002','2026-07-10'),
(8,'TXN-008-RAHUL2',8,399,'SUCCESSFUL','RAZORPAY','GW-RAHUL002','2026-07-15');

-- ── REFUNDS ───────────────────────────────────────────────────────────
INSERT IGNORE INTO refunds (payment_id,refund_amount,refund_status,reason,refund_reference,refund_date) VALUES
(6,99,'COMPLETED','Event cancelled due to venue issue','REF-ARUN001','2026-07-12'),
(7,99,'PROCESSING','Participant unavailable on event day','REF-PRIYA001','2026-07-11');

-- ── RATINGS ───────────────────────────────────────────────────────────
INSERT IGNORE INTO event_ratings (event_id,user_id,booking_id,overall_rating,review_text,is_verified_attendance,moderation_status,sentiment,created_at,updated_at) VALUES
(11,1,6,5,'Amazing startup pitch event! Great mentors and very well organized.',1,'APPROVED','POSITIVE','2026-07-16',NOW()),
(11,2,7,4,'Good event overall, but the schedule was slightly delayed.',1,'APPROVED','POSITIVE','2026-07-16',NOW()),
(12,3,8,5,'Best IoT workshop I have attended. Very hands-on and practical.',1,'APPROVED','POSITIVE','2026-07-21',NOW());

-- ── NETWORKING CONNECTIONS ────────────────────────────────────────────
INSERT IGNORE INTO networking_connections (requester_id,receiver_id,status,match_reason,match_score,created_at,updated_at) VALUES
(1,2,'ACCEPTED','Same department interest: CSE · Shared AI/ML interests',85,'2026-07-05',NOW()),
(1,5,'ACCEPTED','Same college: PSG College of Technology · Both attended Hackathon',90,'2026-07-06',NOW()),
(2,3,'PENDING','Similar interests: Data Science and Coding',75,'2026-07-07',NOW()),
(3,4,'ACCEPTED','Attended same events · Complementary skills: Backend + Frontend',70,'2026-07-08',NOW());

-- ── CERTIFICATES (for completed events) ───────────────────────────────
INSERT IGNORE INTO certificates (certificate_id,event_id,user_id,recipient_name,college_name,department_name,event_name,issued_at,status,email_sent,created_at) VALUES
('CERT-ARUN-STARTUP01',11,1,'Arun Kumar','PSG College of Technology','CSE','Startup Pitch Day','2026-07-15 18:00:00','GENERATED',1,'2026-07-15'),
('CERT-PRIYA-STARTUP01',11,2,'Priya Sharma','Anna University','IT','Startup Pitch Day','2026-07-15 18:00:00','GENERATED',1,'2026-07-15'),
('CERT-RAHUL-IOT01',12,3,'Rahul Verma','VIT University','CSE','IoT Workshop','2026-07-20 18:00:00','GENERATED',1,'2026-07-20');

-- ── ORGANIZER SCORES ─────────────────────────────────────────────────
INSERT IGNORE INTO organizer_scores (organizer_id,average_rating,total_events,completed_events,total_registrations,attendance_rate,overall_score,badge,updated_at) VALUES
(1,4.5,3,1,15,90.0,82.5,'GOLD',NOW()),
(2,4.0,2,0,8,0,65.0,'SILVER',NOW()),
(3,4.8,4,1,20,95.0,88.0,'GOLD',NOW()),
(4,3.5,2,0,6,0,45.0,'BRONZE',NOW()),
(5,4.2,3,1,12,88.0,75.0,'SILVER',NOW());

-- ── DIRECT MESSAGES ───────────────────────────────────────────────────
INSERT IGNORE INTO direct_messages (sender_id,receiver_id,content,is_read,sent_at) VALUES
(1,5,'Hey! I saw you are also attending CodeStorm Hackathon. Want to team up?',1,'2026-07-09 10:00:00'),
(5,1,'Absolutely! I was looking for a team. What is your tech stack?',1,'2026-07-09 10:05:00'),
(1,5,'I work with React and Spring Boot. You?',1,'2026-07-09 10:07:00'),
(5,1,'Great match! I do Spring Boot too. Let us connect on LinkedIn as well.',0,'2026-07-09 10:10:00'),
(3,4,'Hi Sneha! Great to connect. Are you attending the Web Dev Bootcamp?',1,'2026-07-08 15:00:00'),
(4,3,'Yes! Looking forward to it. See you there.',0,'2026-07-08 15:05:00');

SELECT 'Mock data inserted successfully' AS status;
SELECT COUNT(*) AS organizer_count FROM organizers;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS event_count FROM events;
SELECT COUNT(*) AS booking_count FROM bookings;
SELECT COUNT(*) AS certificate_count FROM certificates;
