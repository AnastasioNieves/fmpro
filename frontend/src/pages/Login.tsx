import { FormEvent, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api, ApiError, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Alert, Button, Card, Input, Select, Spinner } from '../components/ui';
import type { Role, Team } from '../types';

const ROLE_LABELS: Record<string, string> = {
  USER: 'Usuario',
  TRAINER: 'Entrenador',
};

export function Login() {
  const { login, register, user, loading: authLoading } = useAuth();
  const { theme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  type LoginLocationState = {
    from?: string;
    authError?: 'invalid_credentials';
    t?: number;
  };

  const state = location.state as LoginLocationState | null;
  const redirectTo = state?.from ?? '/';
  const baseUrl = import.meta.env.BASE_URL.replace(/\/$/, '');
  const logoUrl = `${baseUrl}/${theme === 'light' ? 'logo-mono-dark.webp' : 'logo-primary.webp'}`;

  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [roleId, setRoleId] = useState('');
  const [roles, setRoles] = useState<Role[]>([]);
  const [publicTeams, setPublicTeams] = useState<Team[]>([]);
  const [teamId, setTeamId] = useState('');
  const [loadingRoles, setLoadingRoles] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!authLoading && user) navigate(redirectTo, { replace: true });
  }, [user, authLoading, navigate, redirectTo]);

  useEffect(() => {
    if (state?.authError !== 'invalid_credentials') return;
    setMode('login');
    setPassword('');
    setError('Usuario o contraseña incorrectos.');
    navigate('/login', { replace: true, state: { from: state.from } });
  }, [state?.authError, state?.from, navigate]);

  useEffect(() => {
    if (mode !== 'register') return;

    setLoadingRoles(true);
    Promise.all([
      api.get<Role[]>(endpoints.rolesRegisterable),
      api.get<Team[]>(endpoints.teamsPublic),
    ])
      .then(([roleData, teamData]) => {
        setRoles(roleData);
        setPublicTeams(teamData);
        if (roleData.length > 0) {
          setRoleId(String(roleData[0].id));
        }
        if (teamData.length > 0) {
          setTeamId(String(teamData[0].id));
        }
      })
      .catch(() => setError('No se pudieron cargar los datos. ¿Está el backend en marcha?'))
      .finally(() => setLoadingRoles(false));
  }, [mode]);

  const selectedRoleName = roles.find((r) => String(r.id) === roleId)?.name?.toUpperCase() ?? '';

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (mode === 'login') {
        await login(username.trim(), password);
      } else {
        if (!roleId) {
          throw new Error('Selecciona un rol');
        }
        if (selectedRoleName === 'USER' && !teamId) {
          throw new Error('Selecciona el equipo que quieres seguir');
        }
        await register({
          username: username.trim(),
          password,
          roleId: roleId,
          teamId: selectedRoleName === 'USER' ? teamId : undefined,
        });
      }
      navigate(redirectTo, { replace: true });
    } catch (err) {
      if (mode === 'login' && err instanceof ApiError && err.status === 401) {
        setPassword('');
        navigate('/login', {
          replace: true,
          state: { from: redirectTo, authError: 'invalid_credentials', t: Date.now() },
        });
        return;
      }

      if (mode === 'login') {
        setError('No se pudo iniciar sesión. Inténtalo de nuevo.');
      } else {
        setError(err instanceof ApiError ? err.message : 'Error de autenticación');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-page">
      <Card className="auth-card">
        <header className="auth-card__header">
          <span className="sidebar__logo" aria-hidden>
            <img className="app-logo__img" src={logoUrl} alt="" />
          </span>
          <h2>{mode === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}</h2>
          <p>Accede al panel de gestión FMPRO</p>
        </header>

        <form className="form" onSubmit={handleSubmit}>
          <Input
            label="Usuario"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
          />
          <Input
            label="Contraseña"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            minLength={4}
            required
          />

          {mode === 'register' && (
            loadingRoles ? (
              <div className="muted page__center">
                <Spinner />
              </div>
            ) : (
              <Select
                label="Rol"
                value={roleId}
                onChange={(e) => setRoleId(e.target.value)}
                required
              >
                {roles.map((role) => (
                  <option key={role.id} value={role.id}>
                    {ROLE_LABELS[role.name] ?? role.name}
                  </option>
                ))}
              </Select>
            )
          )}

          {mode === 'register' && selectedRoleName === 'USER' && !loadingRoles && (
            <Select
              label="Equipo a seguir"
              value={teamId}
              onChange={(e) => setTeamId(e.target.value)}
              required
            >
              {publicTeams.map((team) => (
                <option key={team.id} value={String(team.id)}>
                  {team.name}
                </option>
              ))}
            </Select>
          )}

          {error && <Alert>{error}</Alert>}

          <Button type="submit" disabled={loading || (mode === 'register' && !roleId)}>
            {loading ? 'Espere…' : mode === 'login' ? 'Entrar' : 'Registrarse'}
          </Button>
        </form>

        <p className="auth-card__switch">
          {mode === 'login' ? '¿No tienes cuenta?' : '¿Ya tienes cuenta?'}{' '}
          <button
            type="button"
            className="link-btn"
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login');
              setError(null);
            }}
          >
            {mode === 'login' ? 'Regístrate' : 'Inicia sesión'}
          </button>
        </p>

        <Link to="/" className="auth-card__back">
          Volver al inicio
        </Link>
      </Card>
    </section>
  );
}
