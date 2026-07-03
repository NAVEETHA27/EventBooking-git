import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

/**
 * Convenience hook — shorthand for useContext(AuthContext).
 * Usage: const { user, loading, login, logout } = useAuth();
 */
export function useAuth() {
  return useContext(AuthContext);
}
