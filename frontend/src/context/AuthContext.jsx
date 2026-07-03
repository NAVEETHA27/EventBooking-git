import { createContext, useContext, useState, useEffect, useCallback } from 'react';
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

export function AuthProvider({ children }) {
  const [user, setUser]       = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('eb_user');
      const token  = localStorage.getItem('eb_token');
      if (stored && token) {
        const parsed = JSON.parse(stored);
        if (parsed?.email) {
          setUser(parsed);
          api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }
      }
    } catch {
      localStorage.removeItem('eb_token');
      localStorage.removeItem('eb_user');
    } finally {
      setLoading(false);
    }
  }, []);

  const login = useCallback((authResponse) => {
    const { accessToken, refreshToken, user: info } = authResponse;

    // Persist tokens
    localStorage.setItem('eb_token', accessToken);
    localStorage.setItem('eb_user', JSON.stringify(info));
    if (refreshToken) localStorage.setItem('eb_refresh_token', refreshToken);
    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
    setUser(info);

    refreshAuthenticatedData(info).catch((err) => {
      console.warn('Could not prefetch fresh session data', err);
      queryClient.invalidateQueries();
    });
    resetChatSession();
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('eb_token');
    localStorage.removeItem('eb_user');
    localStorage.removeItem('eb_refresh_token');
    delete api.defaults.headers.common['Authorization'];
    setUser(null);

    // Clear all cached query data so stale user data is never shown
    queryClient.clear();

    resetChatSession();

    toast.info('Signed out successfully.');
  }, []);

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
