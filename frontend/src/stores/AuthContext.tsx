import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import api, { AUTH_TOKEN_KEY } from '../api';
import { getJwtRoles, getJwtUsername, isJwtExpired } from '../utils/jwt';

interface AuthContextState {
  token: string | null;
  username: string;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isManager: boolean;
  roles: string[];
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    const stored = localStorage.getItem(AUTH_TOKEN_KEY);
    if (stored && !isJwtExpired(stored)) {
      return stored;
    }
    localStorage.removeItem(AUTH_TOKEN_KEY);
    return null;
  });
  const roles = useMemo(() => (token ? getJwtRoles(token) : []), [token]);

  // localStorage is updated synchronously (not in an effect) because api.ts reads it on every request
  const login = (newToken: string) => {
    localStorage.setItem(AUTH_TOKEN_KEY, newToken);
    setToken(newToken);
  };

  const logout = () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    setToken(null);
  };

  // Backend returns 401 when the token is missing or expired: drop it so the user is sent to /login
  useEffect(() => {
    const id = api.interceptors.response.use(undefined, (error) => {
      if (error.response?.status === 401) {
        logout();
      }
      return Promise.reject(error);
    });
    return () => api.interceptors.response.eject(id);
  }, []);

  const value = useMemo(
    () => ({
      token,
      username: token ? getJwtUsername(token) : '',
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
