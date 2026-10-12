import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import api, { setAuthToken } from '../api';
import { getJwtRoles, isJwtExpired } from '../utils/jwt';

interface AuthContextState {
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isManager: boolean;
  roles: string[];
  login: (token: string) => void;
  logout: () => void;
}

const AUTH_TOKEN_KEY = 'employeehub_token';
const AuthContext = createContext<AuthContextState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const stored = localStorage.getItem(AUTH_TOKEN_KEY);
    return stored && !isJwtExpired(stored) ? stored : null;
  });
  const roles = useMemo(() => (token ? getJwtRoles(token) : []), [token]);

  useEffect(() => {
    if (token) {
      localStorage.setItem(AUTH_TOKEN_KEY, token);
      setAuthToken(token);
    } else {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      setAuthToken(null);
    }
  }, [token]);

  // Backend returns 401 when the token is missing or expired: drop it so the user is sent to /login
  useEffect(() => {
    const id = api.interceptors.response.use(undefined, (error) => {
      if (error.response?.status === 401) {
        setToken(null);
      }
      return Promise.reject(error);
    });
    return () => api.interceptors.response.eject(id);
  }, []);

  const login = (newToken: string) => {
    setToken(newToken);
  };

  const logout = () => {
    setToken(null);
  };

  const value = useMemo(
    () => ({
      token,
      isAuthenticated: Boolean(token),
      roles,
      isAdmin: roles.includes('ROLE_ADMIN'),
      isManager: roles.includes('ROLE_MANAGER'),
      login,
      logout,
    }),
    [token, roles]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
