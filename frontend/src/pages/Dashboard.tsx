import { Link } from 'react-router-dom';
import { Calendar, Shield, TrendingUp, Users } from 'lucide-react';
import { api, endpoints } from '../api/client';
import { Alert, Card, Spinner } from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Match, Player, StatisticsSummary, Team } from '../types';

export function Dashboard() {
  const teams = useFetch(() => api.get<Team[]>(endpoints.teams), []);
  const players = useFetch(() => api.get<Player[]>(endpoints.players), []);
  const matches = useFetch(() => api.get<Match[]>(endpoints.matches), []);
  const stats = useFetch(
    () => api.get<StatisticsSummary>(endpoints.statisticsSummary),
    [],
  );

  const loading = teams.loading || players.loading || matches.loading || stats.loading;
  const error = teams.error || players.error || matches.error || stats.error;

  const totalGoals = stats.data?.totalGoals ?? 0;

  const metrics = [
    {
      label: 'Equipos',
      value: teams.data?.length ?? 0,
      icon: Shield,
      to: '/equipos',
      color: 'emerald',
    },
    {
      label: 'Jugadores',
      value: players.data?.length ?? 0,
      icon: Users,
      to: '/jugadores',
      color: 'sky',
    },
    {
      label: 'Partidos',
      value: matches.data?.length ?? 0,
      icon: Calendar,
      to: '/partidos',
      color: 'amber',
    },
    {
      label: 'Goles registrados',
      value: totalGoals,
      icon: TrendingUp,
      to: '/estadisticas',
      color: 'rose',
    },
  ];

  const upcoming = [...(matches.data ?? [])]
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, 4);

  return (
    <div className="page">
      <section className="hero">
        <div className="hero__content">
          <span className="hero__eyebrow">Panel de control</span>
          <h2>Tu club, bajo control total</h2>
          <p>
            Administra plantillas, convocatorias y rendimiento desde una interfaz
            pensada para entrenadores y directivos deportivos.
          </p>
          <div className="hero__actions">
            <Link to="/equipos" className="btn btn--primary">
              Gestionar equipos
            </Link>
            <Link to="/partidos" className="btn btn--secondary">
              Ver partidos
            </Link>
          </div>
        </div>
        <div className="hero__pitch" aria-hidden />
      </section>

      {error && <Alert>{error}</Alert>}

      {loading ? (
        <div className="page__center">
          <Spinner />
        </div>
      ) : (
        <>
          <div className="metrics">
            {metrics.map(({ label, value, icon: Icon, to, color }) => (
              <Link key={label} to={to} className={`metric metric--${color}`}>
                <span className="metric__icon">
                  <Icon size={20} />
                </span>
                <span className="metric__value">{value}</span>
                <span className="metric__label">{label}</span>
              </Link>
            ))}
          </div>

          <Card title="Próximos partidos" subtitle="Últimas convocatorias registradas">
            {upcoming.length === 0 ? (
              <p className="muted">Aún no hay partidos. Crea uno en la sección Partidos.</p>
            ) : (
              <ul className="list">
                {upcoming.map((match) => (
                  <li key={match.id} className="list__item">
                    <div>
                      <strong>{match.location}</strong>
                      <span className="muted">
                        {new Date(match.date).toLocaleString('es-ES', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })}
                      </span>
                    </div>
                    <span className="badge">
                      {match.lineup?.length ?? 0} jugadores
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
