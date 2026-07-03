import { Suspense, lazy } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './layouts/Layout';
import Spinner from './components/common/Spinner';

const Landing          = lazy(() => import('./pages/Landing'));
const Events           = lazy(() => import('./pages/Events'));
const EventDetail      = lazy(() => import('./pages/EventDetail'));
const Login            = lazy(() => import('./pages/auth/Login'));
const Register         = lazy(() => import('./pages/auth/Register'));
const ForgotPassword   = lazy(() => import('./pages/auth/ForgotPassword'));
const ResetPassword    = lazy(() => import('./pages/auth/ResetPassword'));
const VerifyEmail      = lazy(() => import('./pages/auth/VerifyEmail'));
const OtpVerify        = lazy(() => import('./pages/auth/OtpVerify'));

const UserDashboard    = lazy(() => import('./pages/user/Dashboard'));
const MyBookings       = lazy(() => import('./pages/user/MyBookings'));
const BookingDetail    = lazy(() => import('./pages/user/BookingDetail'));
const PaymentCheckout  = lazy(() => import('./pages/user/PaymentCheckout'));
const UserProfile      = lazy(() => import('./pages/user/Profile'));
const Notifications    = lazy(() => import('./pages/user/Notifications'));
const PaymentHistory   = lazy(() => import('./pages/user/PaymentHistory'));
const RefundTracking   = lazy(() => import('./pages/user/RefundTracking'));
const QueueStatus      = lazy(() => import('./pages/user/QueueStatus'));
const HelpCenter       = lazy(() => import('./pages/HelpCenter'));

const OrgDashboard     = lazy(() => import('./pages/organizer/Dashboard'));
const CreateEvent      = lazy(() => import('./pages/organizer/CreateEvent'));
const EditEvent        = lazy(() => import('./pages/organizer/EditEvent'));
const MyEvents         = lazy(() => import('./pages/organizer/MyEvents'));
const OrgProfile       = lazy(() => import('./pages/organizer/Profile'));
const Attendees        = lazy(() => import('./pages/organizer/Attendees'));
const AdminDashboard   = lazy(() => import('./pages/admin/Dashboard'));
const AdminApprovals   = lazy(() => import('./pages/admin/Approvals'));

const NotFound         = lazy(() => import('./pages/NotFound'));

function Wrap({ children }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}>
      {children}
    </motion.div>
  );
}

function PrivateRoute({ children, roles }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner full />;
  if (!user)   return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function GuestRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner full />;
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : user.role === 'ORGANIZER' ? '/organizer/dashboard' : '/dashboard'} replace />;
  return children;
}

function AppRoutes() {
  const loc = useLocation();
  return (
    <AnimatePresence mode="wait">
      <Routes location={loc} key={loc.pathname}>
        <Route element={<Layout />}>
          {/* Public */}
          <Route path="/"             element={<Wrap><Landing /></Wrap>} />
          <Route path="/events"       element={<Wrap><Events /></Wrap>} />
          <Route path="/events/:id"   element={<Wrap><EventDetail /></Wrap>} />
          <Route path="/help"         element={<Wrap><HelpCenter /></Wrap>} />
          <Route path="/verify-email" element={<Wrap><VerifyEmail /></Wrap>} />

          {/* Auth */}
          <Route path="/login"           element={<GuestRoute><Wrap><Login /></Wrap></GuestRoute>} />
          <Route path="/register"        element={<GuestRoute><Wrap><Register /></Wrap></GuestRoute>} />
          <Route path="/forgot-password" element={<GuestRoute><Wrap><ForgotPassword /></Wrap></GuestRoute>} />
          <Route path="/reset-password"  element={<GuestRoute><Wrap><ResetPassword /></Wrap></GuestRoute>} />
          <Route path="/verify-otp/user"       element={<Wrap><OtpVerify defaultRole="user" /></Wrap>} />
          <Route path="/verify-otp/organizer" element={<Wrap><OtpVerify defaultRole="organizer" /></Wrap>} />
          <Route path="/verify-otp"           element={<Wrap><OtpVerify /></Wrap>} />

          {/* User */}
          <Route path="/dashboard"     element={<PrivateRoute roles={['USER']}><Wrap><UserDashboard /></Wrap></PrivateRoute>} />
          <Route path="/bookings"      element={<PrivateRoute roles={['USER']}><Wrap><MyBookings /></Wrap></PrivateRoute>} />
          <Route path="/bookings/:id"  element={<PrivateRoute roles={['USER']}><Wrap><BookingDetail /></Wrap></PrivateRoute>} />
          <Route path="/checkout/:bookingId" element={<PrivateRoute roles={['USER']}><Wrap><PaymentCheckout /></Wrap></PrivateRoute>} />
          <Route path="/profile"       element={<PrivateRoute roles={['USER']}><Wrap><UserProfile /></Wrap></PrivateRoute>} />
          <Route path="/notifications" element={<PrivateRoute roles={['USER', 'ORGANIZER']}><Wrap><Notifications /></Wrap></PrivateRoute>} />
          <Route path="/payments"      element={<PrivateRoute roles={['USER']}><Wrap><PaymentHistory /></Wrap></PrivateRoute>} />
          <Route path="/refunds"       element={<PrivateRoute roles={['USER']}><Wrap><RefundTracking /></Wrap></PrivateRoute>} />
          <Route path="/queue-status"  element={<PrivateRoute roles={['USER']}><Wrap><QueueStatus /></Wrap></PrivateRoute>} />

          {/* Organizer */}
          <Route path="/organizer/dashboard"       element={<PrivateRoute roles={['ORGANIZER']}><Wrap><OrgDashboard /></Wrap></PrivateRoute>} />
          <Route path="/organizer/events"          element={<PrivateRoute roles={['ORGANIZER']}><Wrap><MyEvents /></Wrap></PrivateRoute>} />
          <Route path="/organizer/events/create"   element={<PrivateRoute roles={['ORGANIZER']}><Wrap><CreateEvent /></Wrap></PrivateRoute>} />
          <Route path="/organizer/events/:id/edit" element={<PrivateRoute roles={['ORGANIZER']}><Wrap><EditEvent /></Wrap></PrivateRoute>} />
          <Route path="/organizer/attendees"       element={<PrivateRoute roles={['ORGANIZER']}><Wrap><Attendees /></Wrap></PrivateRoute>} />
          <Route path="/organizer/profile"         element={<PrivateRoute roles={['ORGANIZER']}><Wrap><OrgProfile /></Wrap></PrivateRoute>} />
          <Route path="/admin/dashboard"           element={<PrivateRoute roles={['ADMIN']}><Wrap><AdminDashboard /></Wrap></PrivateRoute>} />
          <Route path="/admin/approvals"           element={<PrivateRoute roles={['ADMIN']}><Wrap><AdminApprovals /></Wrap></PrivateRoute>} />
        </Route>
        <Route path="*" element={<Wrap><NotFound /></Wrap>} />
      </Routes>
    </AnimatePresence>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Suspense fallback={<Spinner full />}>
        <AppRoutes />
      </Suspense>
    </AuthProvider>
  );
}
