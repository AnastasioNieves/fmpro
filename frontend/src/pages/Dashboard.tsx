import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Shield, TrendingUp, Users, ChevronLeft, ChevronRight } from 'lucide-react';
import { api, endpoints } from '../api/client';
import { Alert, Card, Spinner } from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Match, Player, StatisticsSummary, Team, Statistic } from '../types';
import { FUTCard } from '../components/FUTCard';

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



  const [teamStats, setTeamStats] = useState<Statistic[]>([]);
  useEffect(() => {
    if (currentTeam?.id) {
      api.get<Statistic[]>(endpoints.statisticsByTeam(currentTeam.id))
        .then(setTeamStats)
        .catch(console.error);
    }
  }, [currentTeam?.id]);

  const futPlayers = useMemo(() => {
    if (!teamStats || teamStats.length === 0 || !players.data) return { bestByPos: [], keyPlayers: [] };
    
    const byPlayer = new Map<string, any>();
    teamStats.forEach(s => {
      const id = String(s.player?.id ?? '');
      if (!id) return;
      const p = players.data?.find(pl => String(pl.id) === id);
      if (!p) return;
      
      const current = byPlayer.get(id) || {
        id, name: p.name, position: p.position, photoUrl: p.photoUrl,
        goals: 0, assists: 0, minutes: 0, shotsOnTarget: 0, shotsTotal: 0,
        passesCompleted: 0, passesTotal: 0, duelsWon: 0, duelsTotal: 0,
        interceptions: 0, saves: 0,
      };
      
      current.goals += s.goals || 0;
      current.assists += s.assists || 0;
      current.minutes += s.minutesPlayed || 0;
      current.shotsOnTarget += s.shotsOnTarget || 0;
      current.shotsTotal += s.shotsTotal || 0;
      current.passesCompleted += s.passesCompleted || 0;
      current.passesTotal += s.passesTotal || 0;
      current.duelsWon += s.duelsWon || 0;
      current.duelsTotal += s.duelsTotal || 0;
      current.interceptions += s.interceptions || 0;
      current.saves += s.saves || 0;
      
      byPlayer.set(id, current);
    });

    const arr = Array.from(byPlayer.values());
    if (arr.length === 0) return { bestByPos: [], keyPlayers: [] };

    const scoredArr = arr.map(p => {
      const isGK = p.position.toLowerCase().includes('por');
      const score = p.goals * 20 + p.assists * 15 + (isGK ? p.saves * 0.5 : 0) + p.interceptions * 3 + p.minutes / 10;
      return { ...p, score };
    }).sort((a,b) => b.score - a.score);
    
    const keyPlayers = scoredArr.filter(p => !p.position.toLowerCase().includes('por')).slice(0, 2);

    const bestGK = scoredArr.filter(p => p.position.toLowerCase().includes('por')).sort((a,b) => b.score - a.score)[0];
    const bestDEF = scoredArr.filter(p => p.position.toLowerCase().includes('def')).sort((a,b) => b.score - a.score)[0];
    const bestMID = scoredArr.filter(p => p.position.toLowerCase().includes('cen')).sort((a,b) => b.score - a.score)[0];
    const bestFWD = scoredArr.filter(p => p.position.toLowerCase().includes('del')).sort((a,b) => b.score - a.score)[0];

    return {
      keyPlayers,
      bestByPos: [bestGK, bestDEF, bestMID, bestFWD].filter(Boolean)
    };
  }, [teamStats, players.data]);

  const mapToFUT = (p: any) => {
    const isGK = p.position.toLowerCase().includes('por');
    const rating = Math.min(99, Math.max(65, 70 + Math.floor((p.goals * 5 + p.assists * 3 + (isGK ? p.saves * 0.2 : 0) + p.interceptions * 2) / 5)));
    if (isGK) {
      return {
        name: p.name, position: 'POR', photoUrl: p.photoUrl, rating,
        stats: {
          label1: 'PAR', val1: p.saves,
          label2: 'PAS', val2: p.passesTotal,
          label3: 'INT', val3: p.interceptions,
          label4: 'MIN', val4: p.minutes,
          label5: 'P.%', val5: p.passesTotal ? Math.round(p.passesCompleted/p.passesTotal*100) : 0,
          label6: 'D.%', val6: p.duelsTotal ? Math.round(p.duelsWon/p.duelsTotal*100) : 0,
        }
      };
    } else {
      return {
        name: p.name, position: p.position.substring(0,3).toUpperCase(), photoUrl: p.photoUrl, rating,
        stats: {
          label1: 'PAC', val1: Math.min(99, 70 + (p.name.length * 2)),
          label2: 'SHO', val2: Math.min(99, p.goals * 5 + p.shotsOnTarget * 2),
          label3: 'PAS', val3: p.passesTotal ? Math.round(p.passesCompleted/p.passesTotal*100) : 0,
          label4: 'DRI', val4: Math.min(99, 65 + p.duelsWon * 2),
          label5: 'DEF', val5: Math.min(99, 50 + p.interceptions * 3),
          label6: 'PHY', val6: Math.min(99, 60 + Math.floor(p.minutes / 50)),
        }
      };
    }
  };

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

          {futPlayers.keyPlayers.length > 0 && (
            <div style={{ marginTop: '3rem' }}>
              <h2 style={{ fontSize: '1.5rem', marginBottom: '1rem', color: 'var(--text)' }}>Jugadores Clave (MVPs)</h2>
              <div style={{ display: 'flex', gap: '2rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                {futPlayers.keyPlayers.map(p => (
                  <FUTCard key={p.id} {...mapToFUT(p)} variant="totw" />
                ))}
              </div>
            </div>
          )}

          {futPlayers.bestByPos.length > 0 && (
            <div style={{ marginTop: '3rem' }}>
              <h2 style={{ fontSize: '1.5rem', marginBottom: '1rem', color: 'var(--text)' }}>Mejores por Posición</h2>
              <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
                {futPlayers.bestByPos.map(p => (
                  <FUTCard key={p.id} {...mapToFUT(p)} variant="gold" />
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
