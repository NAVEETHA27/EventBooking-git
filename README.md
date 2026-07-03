# Event Booking System

Full-stack college event ticket booking platform.
- **Backend**: Spring Boot 3.2 · MySQL · MongoDB · JWT · Razorpay
- **Frontend**: React 18 · Vite · Tailwind CSS · React Query

---

## Quick Start

### Backend
```bash
cd backend
# Set env vars (or edit application.yml)
set DB_PASSWORD=yourpassword
set MAIL_USERNAME=you@gmail.com
set MAIL_PASSWORD=apppassword
mvn spring-boot:run
# Runs on http://localhost:8080/api
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
```

---

## Architecture

```
Controller → Service → Repository → Entity → Database
                ↑
              Mapper (DTO conversion)
```

### Backend Packages
| Package | Purpose |
|---|---|
| `controller/` | REST endpoints — thin, delegate to services |
| `service/` | Business logic |
| `repository/` | Spring Data JPA / MongoDB repos |
| `entity/` | JPA entities (MySQL) |
| `entity/mongo/` | MongoDB documents |
| `dto/request/` | Inbound API payloads |
| `dto/response/` | Outbound API payloads |
| `mapper/` | Entity ↔ DTO conversion |
| `security/` | JWT filter, token provider, OTP/refresh stores |
| `config/` | SecurityConfig, WebConfig, MockDataSeeder |
| `exception/` | Custom exceptions + GlobalExceptionHandler |
| `scheduler/` | `@Scheduled` tasks (EventScheduler, BookingScheduler) |
| `notification/` | SSE-based real-time notifications + MongoDB persistence |
| `payment/` | Razorpay integration, refund management |
| `ai/` | AI chatbot (OpenAI / Gemini providers) |
| `util/` | QR code generator, ticket ID generator |
| `validation/` | Custom Bean Validation annotations |

### Frontend Structure
```
src/
  api/          # (reserved for per-domain API modules)
  assets/       # Static images
  components/
    common/     # Shared UI components (EventCard, Spinner, ErrorBoundary…)
    layout/     # Layout sub-components
  context/      # AuthContext, ThemeContext
  hooks/        # useAuth, useDebounce
  layouts/      # Navbar, Footer, Layout wrapper
  pages/
    auth/       # Login, Register, OTP, ForgotPassword…
    user/       # Dashboard, Bookings, Payments, Profile…
    organizer/  # Dashboard, CreateEvent, EditEvent…
    admin/      # Dashboard, Approvals
  routes/       # ROUTES constants
  services/     # api.js — axios instance + all API calls
  styles/       # globals.css
  utils/        # formatters.js (dates, currency, status colours)
  App.jsx       # Router + route guards
  main.jsx      # React root, QueryClient, BrowserRouter
```

---

## Roles
| Role | Access |
|---|---|
| `USER` | Browse events, book tickets, payments, refunds, profile |
| `ORGANIZER` | Create/manage events, view attendees, dashboard |
| `ADMIN` | Approve events, manage users/organizers, audit logs |

## Key Endpoints
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/user/login` | User login |
| POST | `/api/auth/organizer/login` | Organizer login |
| GET | `/api/events` | Search/filter events |
| POST | `/api/bookings` | Book tickets |
| POST | `/api/payments/bookings/{id}/razorpay/order` | Create Razorpay order |
| GET | `/api/notifications` | User notifications |
| GET | `/api/notifications/stream` | SSE stream |
| POST | `/api/ai/chat` | AI chatbot |
| GET | `/api/admin/dashboard` | Admin stats |
