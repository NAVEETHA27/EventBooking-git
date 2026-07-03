/**
 * Route configuration is defined in App.jsx (co-located with AuthProvider).
 * This file re-exports the route helpers so other modules can import from
 * a single, predictable path without touching App.jsx.
 *
 * Usage:
 *   import { ROUTES } from '../routes/AppRoutes';
 *   navigate(ROUTES.LOGIN);
 */

export const ROUTES = {
  // Public
  HOME:            '/',
  EVENTS:          '/events',
  EVENT_DETAIL:    (id = ':id') => `/events/${id}`,
  HELP:            '/help',
  VERIFY_EMAIL:    '/verify-email',

  // Auth
  LOGIN:           '/login',
  REGISTER:        '/register',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD:  '/reset-password',
  VERIFY_OTP:      '/verify-otp',

  // User
  DASHBOARD:       '/dashboard',
  BOOKINGS:        '/bookings',
  BOOKING_DETAIL:  (id = ':id') => `/bookings/${id}`,
  CHECKOUT:        (id = ':bookingId') => `/checkout/${id}`,
  PROFILE:         '/profile',
  NOTIFICATIONS:   '/notifications',
  PAYMENTS:        '/payments',
  REFUNDS:         '/refunds',
  QUEUE_STATUS:    '/queue-status',

  // Organizer
  ORG_DASHBOARD:   '/organizer/dashboard',
  ORG_EVENTS:      '/organizer/events',
  ORG_CREATE:      '/organizer/events/create',
  ORG_EDIT:        (id = ':id') => `/organizer/events/${id}/edit`,
  ORG_ATTENDEES:   '/organizer/attendees',
  ORG_PROFILE:     '/organizer/profile',

  // Admin
  ADMIN_DASHBOARD: '/admin/dashboard',
  ADMIN_APPROVALS: '/admin/approvals',
};
