import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api, ApiError, endpoints } from '../api/client';
import type { AuthUser, UserRequest, UserResponse } from '../types';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  isAdmin: boolean;
  isUser: boolean;
  canManageMatches: boolean;
  canExportReports: boolean;
  canManageTeams: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (data: UserRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshSession = useCallback(async () => {
    try {
      const me = await api.get<AuthUser>(endpoints.auth.me);
      setUser(me);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        setUser(null);
      } else {
        setUser(null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshSession();
  }, [refreshSession]);

  const login = useCallback(async (username: string, password: string) => {
    const authenticated = await api.post<AuthUser>(endpoints.auth.login, {
      username,
      password,
    });
    setUser(authenticated);
  }, []);

  const register = useCallback(async (data: UserRequest) => {
    const created = await api.post<UserResponse>(endpoints.auth.register, data);
    setUser({
      id: created.id,
      username: created.username,
      roleId: created.roleId,
      roleName: created.roleName,
      teamId: data.teamId,
    });
    await refreshSession();
  }, [refreshSession]);

  const logout = useCallback(async () => {
    try {
      await api.post(endpoints.auth.logout, {});
    } catch {
      /* sesión ya caducada */
    }
    setUser(null);
  }, []);

  const role = user?.roleName?.toUpperCase() ?? '';
  const isAdmin = role === 'ADMIN';
  const isUser = role === 'USER';
  const canManageMatches = isAdmin || role === 'TRAINER';
  const canExportReports = canManageMatches;
  const canManageTeams = canManageMatches;

  const value = useMemo(
    () => ({
      user,
      loading,
      isAdmin,
      isUser,
      canManageMatches,
      canExportReports,
      canManageTeams,
      login,
      register,
      logout,
    }),
    [
      user,
      loading,
      isAdmin,
      isUser,
      canManageMatches,
      canExportReports,
      canManageTeams,
      login,
      register,
      logout,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}
