import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authService } from '../services/auth.service';

const AuthContext = createContext(null);

/**
 * AuthProvider — manages authentication state across the app.
 * Stores token in localStorage, auto-loads user on mount.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Load user on mount if token exists
  useEffect(() => {
    const token = localStorage.getItem('rentify_token');
    if (token) {
      authService
        .getMe()
        .then((res) => setUser(res.data.user))
        .catch((err) => {
          if (err.status === 401 || err.status === 403) {
            localStorage.removeItem('rentify_token');
            setUser(null);
          }
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  // Listen for global auth errors
  useEffect(() => {
    const handleAuthError = (e) => {
      console.warn('Auth Error Detected, logging out...', e.detail.status);
      localStorage.removeItem('rentify_token');
      setUser(null);
      // Optional: window.location.href = '/login'; // Force redirect
    };

    window.addEventListener('rentify-auth-error', handleAuthError);
    return () => window.removeEventListener('rentify-auth-error', handleAuthError);
  }, []);

  /**
   * Logs in a user and stores token.
   * Input: { email, password }
   * Output: user object
   */
  const login = useCallback(async (credentials) => {
    const res = await authService.login(credentials);
    localStorage.setItem('rentify_token', res.data.token);
    setUser(res.data.user);
    return res.data.user;
  }, []);

  /**
   * Registers a new user and stores token.
   * Input: { name, email, password, campus }
   * Output: user object
   */
  const register = useCallback(async (userData) => {
    const res = await authService.register(userData);
    localStorage.setItem('rentify_token', res.data.token);
    setUser(res.data.user);
    return res.data.user;
  }, []);

  /**
   * Logs out the current user and clears token.
   */
  const logout = useCallback(() => {
    localStorage.removeItem('rentify_token');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to access auth context from any component.
 * Output: { user, loading, login, register, logout }
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
