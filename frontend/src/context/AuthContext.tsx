import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { api, endpoints } from '../api/client';
import type { AuthUser, UserRequest, UserResponse } from '../types';
import { auth } from '../firebase';
import { signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut, onIdTokenChanged } from 'firebase/auth';

const getFirebaseEmail = (username: string) => username.includes('@') ? username : `${username}@fmpro.com`;

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

  useEffect(() => {
    const unsubscribe = onIdTokenChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        try {
          const me = await api.get<AuthUser>(endpoints.auth.me);
          setUser(me);
        } catch (e) {
          console.error("Error fetching user profile", e);
          setUser(null);
        }
      } else {
        setUser(null);
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    await signInWithEmailAndPassword(auth, getFirebaseEmail(username), password);
  }, []);

  const register = useCallback(async (data: UserRequest) => {
    const userCredential = await createUserWithEmailAndPassword(auth, getFirebaseEmail(data.username), data.password);
    const uid = userCredential.user.uid;
    
    try {
      await api.post<UserResponse>(endpoints.auth.register, {
        ...data,
        id: uid
      });
      // onIdTokenChanged se encargará de actualizar el usuario al estar logueado
    } catch (e) {
      await userCredential.user.delete().catch(() => {});
      throw e;
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post(endpoints.auth.logout, {});
    } catch {
      /* ignorar */
    }
    await signOut(auth);
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
