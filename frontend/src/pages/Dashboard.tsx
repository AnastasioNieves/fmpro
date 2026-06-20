import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Shield, TrendingUp, Users, ChevronLeft, ChevronRight } from 'lucide-react';
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

  const [currentTeamIndex, setCurrentTeamIndex] = useState(0);

  useEffect(() => {
    if (!teams.data || teams.data.length <= 1) return;
    const interval = setInterval(() => {
      setCurrentTeamIndex((prev) => (prev + 1) % teams.data!.length);
    }, 30000);
    return () => clearInterval(interval);
  }, [teams.data]);

  const currentTeam = teams.data?.[currentTeamIndex];

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

  const teamMatches = (matches.data ?? []).filter(m => currentTeam ? m.teamId === currentTeam.id : true);
  const finishedMatches = teamMatches.filter((m) => m.status === 'FINISHED');
  const matchesPlayed = finishedMatches.length;
  let matchesWon = 0;
  let matchesLost = 0;
  let matchesDrawn = 0;
  let goalsFor = 0;
  let goalsAgainst = 0;

  finishedMatches.forEach((m) => {
    const ts = m.teamScore ?? 0;
    const os = m.opponentScore ?? 0;
    goalsFor += ts;
    goalsAgainst += os;
    if (ts > os) matchesWon++;
    else if (ts < os) matchesLost++;
    else matchesDrawn++;
  });

  const winPercentage = matchesPlayed > 0 ? Math.round((matchesWon / matchesPlayed) * 100) : 0;
  const goalDifference = goalsFor - goalsAgainst;

  const recentMatches = [...finishedMatches]
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, 5)
    .reverse();

  const recentForm = recentMatches.map(m => {
    const ts = Number(m.teamScore) || 0;
    const os = Number(m.opponentScore) || 0;
    if (ts > os) return 'V';
    if (ts < os) return 'D';
    return 'E';
  });



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
        
        <div className="team-stats-card" style={{ position: 'relative', overflow: 'hidden' }}>
          <div className="team-stats-card__top" style={{ marginBottom: '-0.25rem' }}>
            <span className="team-stats-card__title">Team Statistics</span>
          </div>

          <div key={currentTeamIndex} className="team-stats-card__content" style={{ animation: 'slideInLeft 0.5s cubic-bezier(0.2, 0.8, 0.2, 1)' }}>
            <div className="team-stats-card__header">
              <div className="team-stats-card__team">
                <div className="team-stats-card__logo">
                   <Shield size={28} />
                </div>
                <div className="team-stats-card__team-info">
                  <strong>{currentTeam?.name || 'Resumen Global'}</strong>
                  <span>{matchesPlayed} Jug. • {matchesWon}G {matchesDrawn}E {matchesLost}P</span>
                </div>
              </div>
            </div>
            
            <div className="team-stats-card__form-row">
              <span className="team-stats-card__label">Partidos recientes</span>
              <div className="team-stats-card__form-badges">
                {recentForm.length === 0 ? (
                  <span className="muted" style={{fontSize: '0.8rem'}}>Sin datos</span>
                ) : (
                  recentForm.map((result, i) => (
                    <span key={i} className={`form-badge form-badge--${result}`}>{result}</span>
                  ))
                )}
              </div>
            </div>

            <div className="team-stats-card__metrics">
              <div className="team-stats-card__metric-box">
                <span className="team-stats-card__metric-label">Dif. Goles</span>
                <strong className={`team-stats-card__metric-value ${goalDifference > 0 ? 'text-accent' : goalDifference < 0 ? 'text-danger' : ''}`}>
                  {goalDifference > 0 ? '+' : ''}{goalDifference}
                </strong>
              </div>
              <div className="team-stats-card__metric-box">
                <span className="team-stats-card__metric-label">Victorias</span>
                <strong className="team-stats-card__metric-value text-accent">
                  {winPercentage}%
                </strong>
              </div>
            </div>
          </div>

          {teams.data && teams.data.length > 1 && (
            <div className="team-stats-card__pagination">
              <button 
                className="team-stats-card__nav-btn" 
                onClick={() => setCurrentTeamIndex((prev) => (prev - 1 + teams.data!.length) % teams.data!.length)}
                aria-label="Equipo anterior"
              >
                <ChevronLeft size={16} />
              </button>
              
              <div className="team-stats-card__dots">
                {teams.data.map((_, i) => (
                  <button 
                    key={i} 
                    className={`team-stats-card__dot ${i === currentTeamIndex ? 'team-stats-card__dot--active' : ''}`}
                    onClick={() => setCurrentTeamIndex(i)}
                    aria-label={`Ir al equipo ${i + 1}`}
                  />
                ))}
              </div>

              <button 
                className="team-stats-card__nav-btn" 
                onClick={() => setCurrentTeamIndex((prev) => (prev + 1) % teams.data!.length)}
                aria-label="Siguiente equipo"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </div>
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
