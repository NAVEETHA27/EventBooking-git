import axios from 'axios';
import { toast } from 'react-toastify';

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('eb_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Response interceptor ─────────────────────────────────────────────
api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (!err.response) {
      toast.error('⚠️ Network error — please check your connection.');
      return Promise.reject(err);
    }

    const { status, data } = err.response;
    const config = err.config;

    // Silent token refresh on TOKEN_EXPIRED
    if (status === 401 && data?.errorCode === 'TOKEN_EXPIRED' && !config._retry) {
      config._retry = true;
      try {
        const refreshToken = localStorage.getItem('eb_refresh_token');
        if (!refreshToken) throw new Error('No refresh token');

        const { data: res } = await api.post('/auth/refresh-token', { refreshToken });
        const payload = res.data?.data ?? res.data;
        const { accessToken, refreshToken: newRefresh } = payload;

        localStorage.setItem('eb_token', accessToken);
        if (newRefresh) localStorage.setItem('eb_refresh_token', newRefresh);
        api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
        config.headers['Authorization'] = `Bearer ${accessToken}`;

        return api(config); // retry original request
      } catch {
        // Refresh failed — clear session and redirect
        localStorage.removeItem('eb_token');
        localStorage.removeItem('eb_refresh_token');
        localStorage.removeItem('eb_user');
        delete api.defaults.headers.common['Authorization'];
        window.location.href = '/login';
        return Promise.reject(err);
      }
    }

    // Hard 401 (not TOKEN_EXPIRED) on a protected route — redirect to login
    if (status === 401 && !config._retry) {
      const isAuthRoute = (config?.url ?? '').includes('/auth/');
      if (!isAuthRoute && !window.location.pathname.includes('/login')) {
        localStorage.removeItem('eb_token');
        localStorage.removeItem('eb_refresh_token');
        localStorage.removeItem('eb_user');
        window.location.href = '/login';
      }
    }

    const msg =
      data?.message || data?.error ||
      (typeof data === 'string' ? data : null) ||
      'An unexpected error occurred';

    // Don't toast on 401 auth routes (let the login form handle it)
    const isAuthEndpoint = (config?.url ?? '').includes('/auth/');
    const isOtpEndpoint  = (config?.url ?? '').includes('/auth/otp/');
    if (status !== 404 && !(status === 401 && isAuthEndpoint) && !isOtpEndpoint) {
      toast.error(msg);
    }

    return Promise.reject(err);
  }
);

// ── Auth ─────────────────────────────────────────────────────────────
export const authAPI = {
  userRegister:      (d) => api.post('/auth/user/register', d),
  userLogin:         (d) => api.post('/auth/user/login', d),
  organizerRegister: (d) => api.post('/auth/organizer/register', d),
  organizerLogin:    (d) => api.post('/auth/organizer/login', d),
  verifyEmail:       (token, role) => api.get(`/auth/verify-email?token=${token}&role=${role}`),
  forgotPassword:    (d) => api.post('/auth/forgot-password', d),
  resetPassword:     (d) => api.post('/auth/reset-password', d),
  resetPasswordWithOtp: (d) => api.post('/auth/reset-password/otp', d),
  refreshToken:      (d) => api.post('/auth/refresh-token', d),
  sendOtp:           (d) => api.post('/auth/otp/send', d),
  verifyOtp:         (d) => api.post('/auth/otp/verify', d),
};

// ── Events ───────────────────────────────────────────────────────────
export const eventsAPI = {
  search:      (p)     => api.get('/events', { params: p }),
  featured:    ()      => api.get('/events/featured'),
  getById:     (id)    => api.get(`/events/${id}`),
  categories:  ()      => api.get('/events/categories'),
  create:      (d)     => api.post('/events', d),
  update:      (id, d) => api.put(`/events/${id}`, d),
  delete:      (id)    => api.delete(`/events/${id}`),
  cancel:      (id)    => api.patch(`/events/${id}/cancel`),
  publish:     (id)    => api.patch(`/events/${id}/publish`),
  myEvents:    (p)     => api.get('/events/my', { params: p }),
  uploadBanner:(id, f) => api.post(`/events/${id}/banner`, f, { headers: { 'Content-Type': 'multipart/form-data' } }),
  uploadAuthorizedDocument:(id, f) => api.post(`/events/${id}/authorized-document`, f, { headers: { 'Content-Type': 'multipart/form-data' } }),
};

// ── Bookings ──────────────────────────────────────────────────────────
export const bookingsAPI = {
  book:       (d)     => api.post('/bookings', d),
  myBookings: (p)     => api.get('/bookings', { params: p }),
  getById:    (id)    => api.get(`/bookings/${id}`),
  getByTicket:(tid)   => api.get(`/bookings/ticket/${tid}`),
  cancel:     (id, r) => api.patch(`/bookings/${id}/cancel`, null, { params: { reason: r } }),
};

export const helpAPI = {
  get:    (p)     => api.get('/help', { params: p }),
  faqs:   (p)     => api.get('/help/faqs', { params: p }),
  videos: ()      => api.get('/help/videos'),
  saveVideo: (id, d) => id ? api.put(`/help/videos/${id}`, d) : api.post('/help/videos', d),
  deleteVideo: (id) => api.delete(`/help/videos/${id}`),
};

export const paymentsAPI = {
  history: (p) => api.get('/payments', { params: p }),
  markProcessing: (bookingId, d) => api.post(`/payments/bookings/${bookingId}/processing`, d),
  markSuccess: (bookingId, d) => api.post(`/payments/bookings/${bookingId}/success`, d),
  markFailed: (bookingId, d) => api.post(`/payments/bookings/${bookingId}/failed`, d),
  createRazorpayOrder: (bookingId) => api.post(`/payments/bookings/${bookingId}/razorpay/order`),
  verifyRazorpay: (bookingId, d) => api.post(`/payments/bookings/${bookingId}/razorpay/verify`, d),
  refundsByPayment: (paymentId) => api.get(`/payments/${paymentId}/refunds`),
  myRefunds: () => api.get('/payments/refunds/my'),
};

export const adminAPI = {
  dashboard: () => api.get('/admin/dashboard'),
  approvals: (p) => api.get('/admin/approvals', { params: p }),
  reviewEvent: (eventId, d) => api.post(`/admin/events/${eventId}/review`, d),
  users: (p) => api.get('/admin/users', { params: p }),
  organizers: (p) => api.get('/admin/organizers', { params: p }),
  events: (p) => api.get('/admin/events', { params: p }),
  payments: (p) => api.get('/admin/payments', { params: p }),
  refunds: (p) => api.get('/admin/refunds', { params: p }),
  updateRefundStatus: (id, d) => api.patch(`/admin/refunds/${id}/status`, d),
  auditLogs: (p) => api.get('/admin/audit-logs', { params: p }),
};

// ── User ─────────────────────────────────────────────────────────────
export const userAPI = {
  getProfile:     ()  => api.get('/user/profile'),
  updateProfile:  (d) => api.put('/user/profile', d),
  getLocation:    ()  => api.get('/user/profile/location'),
  updateLocation: (d) => api.put('/user/profile/location', d),
  uploadPicture:  (f) => api.post('/user/profile/picture', f, { headers: { 'Content-Type': 'multipart/form-data' } }),
  changePassword: (d) => api.patch('/user/change-password', d),
};

// ── Organizer ─────────────────────────────────────────────────────────
export const organizerAPI = {
  getProfile:     ()  => api.get('/organizer/profile'),
  updateProfile:  (d) => api.put('/organizer/profile', d),
  getLocation:    ()  => api.get('/organizer/profile/location'),
  updateLocation: (d) => api.put('/organizer/profile/location', d),
  uploadLogo:     (f) => api.post('/organizer/profile/logo', f, { headers: { 'Content-Type': 'multipart/form-data' } }),
  changePassword: (d) => api.patch('/organizer/change-password', d),
  getDashboard:   ()  => api.get('/organizer/dashboard'),
  participants:   ()  => api.get('/organizer/participants'),
};

export const chatbotAPI = {
  ask: (message, history = []) => api.post('/ai/chat', { message, history }),
  legacyAsk: (message) => api.post('/chatbot', { message }),
};

// ── Notifications ─────────────────────────────────────────────────────
export const notificationsAPI = {
  getAll:      (p) => api.get('/notifications', { params: p }),
  getUnread:   ()  => api.get('/notifications/unread'),
  markAllRead: ()  => api.patch('/notifications/read-all'),
  streamUrl:   ()  => `${api.defaults.baseURL}/notifications/stream?token=${encodeURIComponent(localStorage.getItem('eb_token') || '')}`,
};

export default api;
