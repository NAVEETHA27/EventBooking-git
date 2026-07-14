import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import api, { bookingsAPI, eventsAPI, notificationsAPI, organizerAPI, userAPI } from '../services/api';
import { toast } from 'react-toastify';
import { queryClient } from '../main';

const AuthContext = createContext(null);
const CHAT_REFRESH_EVENT = 'eb:chat-refresh';
const getQueryRoot = (queryKey) => Array.isArray(queryKey) ? queryKey[0] : queryKey;

function resetChatSession() {
  sessionStorage.removeItem('eb_ai_chat');
  window.dispatchEvent(new Event(CHAT_REFRESH_EVENT));
}

async function refreshAuthenticatedData(info) {
  await queryClient.cancelQueries();
  queryClient.removeQueries({ predicate: (query) => getQueryRoot(query.queryKey) !== 'categories' });

  const role = info?.role;
  const commonFetches = [
    queryClient.prefetchQuery('notifs', () => notificationsAPI.getAll({ page: 0, size: 50 }).then((r) => r.data?.data)),
  ];

  if (role === 'USER') {
    commonFetches.push(
      queryClient.prefetchQuery('user-profile', () => userAPI.getProfile().then((r) => r.data?.data)),
      queryClient.prefetchQuery(['my-bookings', 0], () => bookingsAPI.myBookings({ page: 0, size: 10 }).then((r) => r.data?.data)),
      queryClient.prefetchQuery('dash-bookings', () => bookingsAPI.myBookings({ page: 0, size: 5 }).then((r) => r.data?.data)),
      queryClient.prefetchQuery('dash-events', () => eventsAPI.featured().then((r) => r.data?.data)),
    );
  }

  if (role === 'ORGANIZER') {
    commonFetches.push(
      queryClient.prefetchQuery('org-profile', () => organizerAPI.getProfile().then((r) => r.data?.data)),
      queryClient.prefetchQuery('org-dash', () => organizerAPI.getDashboard().then((r) => r.data?.data)),
      queryClient.prefetchQuery(['org-events', 0, 'all'], () => eventsAPI.myEvents({ page: 0, size: 10 }).then((r) => r.data?.data)),
      queryClient.prefetchQuery('org-events-dash', () => eventsAPI.myEvents({ page: 0, size: 5 }).then((r) => r.data?.data)),
    );
  }

  await Promise.allSettled(commonFetches);
  await queryClient.invalidateQueries();
}

// ── Session helpers ───────────────────────────────────────────────────────────

/** Parse JWT payload without a library */
function parseJwtExpiry(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp ? payload.exp * 1000 : null; // convert to ms
  } catch {
    return null;
  }
}

/** Read expiresAt from localStorage (set on login/refresh) */
function storedExpiresAt() {
  const v = localStorage.getItem('eb_expires_at');
  return v ? parseInt(v, 10) : null;
}

// ── Provider ──────────────────────────────────────────────────────────────────

export function AuthProvider({ children }) {
  const [user, setUser]       = useState(null);
  const [loading, setLoading] = useState(true);
  const logoutTimerRef        = useRef(null);
  const warnTimerRef          = useRef(null);

  // ── Schedule auto-logout ───────────────────────────────────────────────────
  const scheduleAutoLogout = useCallback((expiresAt, logoutFn) => {
    // Clear any existing timers
    clearTimeout(logoutTimerRef.current);
    clearTimeout(warnTimerRef.current);

    const now = Date.now();
    const msUntilExpiry = expiresAt - now;

    if (msUntilExpiry <= 0) {
      logoutFn(true);
      return;
    }

    // Warn 2 minutes before expiry
    const warnAt = msUntilExpiry - 2 * 60 * 1000;
    if (warnAt > 0) {
      warnTimerRef.current = setTimeout(() => {
        toast.warn('⏳ Your session expires in 2 minutes. Any activity will extend it automatically.', {
          toastId: 'session-expiry-warn',
          autoClose: 10000,
        });
      }, warnAt);
    }

    // Hard logout at expiry
    logoutTimerRef.current = setTimeout(() => {
      logoutFn(true); // true = "expired" flag
    }, msUntilExpiry);
  }, []);

  // ── Logout ─────────────────────────────────────────────────────────────────
  const logout = useCallback((expired = false) => {
    clearTimeout(logoutTimerRef.current);
    clearTimeout(warnTimerRef.current);

    localStorage.removeItem('eb_token');
    localStorage.removeItem('eb_user');
    localStorage.removeItem('eb_refresh_token');
    localStorage.removeItem('eb_expires_at');
    delete api.defaults.headers.common['Authorization'];
    setUser(null);
    queryClient.clear();
    resetChatSession();

    if (expired) {
      toast.error('🔒 Your session has expired. Please sign in again.', {
        toastId: 'session-expired',
        autoClose: 5000,
      });
      // Small delay so toast renders before redirect
      setTimeout(() => { window.location.href = '/login'; }, 500);
    } else {
      toast.info('Signed out successfully.');
    }
  }, []);

  // ── Restore session on mount ───────────────────────────────────────────────
  useEffect(() => {
    try {
      const stored = localStorage.getItem('eb_user');
      const token  = localStorage.getItem('eb_token');
      if (stored && token) {
        const parsed = JSON.parse(stored);
        if (parsed?.email) {
          // Check if token is already expired
          const expiresAt = storedExpiresAt() || parseJwtExpiry(token);
          if (expiresAt && Date.now() >= expiresAt) {
            // Token expired while app was closed — clear and don't restore
            localStorage.removeItem('eb_token');
            localStorage.removeItem('eb_user');
            localStorage.removeItem('eb_refresh_token');
            localStorage.removeItem('eb_expires_at');
          } else {
            setUser(parsed);
            api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            // Re-arm the auto-logout timer for remaining session time
            if (expiresAt) scheduleAutoLogout(expiresAt, logout);
          }
        }
      }
    } catch {
      localStorage.removeItem('eb_token');
      localStorage.removeItem('eb_user');
    } finally {
      setLoading(false);
    }
  }, [scheduleAutoLogout, logout]);

  // ── Listen for token refresh from api.js interceptor ─────────────────────
  // When the axios interceptor silently refreshes the token it fires this event
  useEffect(() => {
    const handler = (e) => {
      const { expiresAt } = e.detail || {};
      if (expiresAt) scheduleAutoLogout(expiresAt, logout);
    };
    window.addEventListener('eb:token-refreshed', handler);
    return () => window.removeEventListener('eb:token-refreshed', handler);
  }, [scheduleAutoLogout, logout]);

  // ── Login ──────────────────────────────────────────────────────────────────
  const login = useCallback((authResponse) => {
    const { accessToken, refreshToken, user: info, expiresIn, expiresAt: serverExpiresAt } = authResponse;

    // Compute absolute expiry — prefer server-provided value, fall back to JWT claim
    const expiresAt = serverExpiresAt
      || (expiresIn ? Date.now() + expiresIn : null)
      || parseJwtExpiry(accessToken);

    localStorage.setItem('eb_token', accessToken);
    localStorage.setItem('eb_user', JSON.stringify(info));
    if (refreshToken) localStorage.setItem('eb_refresh_token', refreshToken);
    if (expiresAt)    localStorage.setItem('eb_expires_at', String(expiresAt));

    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
    setUser(info);

    // Arm auto-logout timer
    if (expiresAt) scheduleAutoLogout(expiresAt, logout);

    refreshAuthenticatedData(info).catch((err) => {
      console.warn('Could not prefetch fresh session data', err);
      queryClient.invalidateQueries();
    });
    resetChatSession();
  }, [scheduleAutoLogout, logout]);

  // ── updateUser ─────────────────────────────────────────────────────────────
  const updateUser = useCallback((updates) => {
    setUser(prev => {
      const next = { ...prev, ...updates };
      localStorage.setItem('eb_user', JSON.stringify(next));
      return next;
    });
    queryClient.invalidateQueries('user-profile');
    queryClient.invalidateQueries('org-profile');
    queryClient.invalidateQueries('dash-bookings');
    queryClient.invalidateQueries('org-dash');
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
