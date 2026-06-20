import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  BarChart3,
  Check,
  Minus,
  Plus,
  Save,
  Shirt,
  Users,
} from 'lucide-react';
import { api, ApiError, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Alert, Badge, Button, Card, Input, Spinner } from '../components/ui';
import type { LiveStat, MatchDetail, MatchScoreUpdate, Player, Team } from '../types';

type Tab = 'summary' | 'squad' | 'lineup' | 'live';

export function MatchDetail() {
  const { id } = useParams<{ id: string }>();
  const matchId = Number(id);
  const { canManageMatches, isUser } = useAuth();

  const [match, setMatch] = useState<MatchDetail | null>(null);
  const [teamName, setTeamName] = useState('Equipo');
  const [allPlayers, setAllPlayers] = useState<Player[]>([]);
  const [tab, setTab] = useState<Tab>(isUser ? 'live' : 'squad');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [selectedSquad, setSelectedSquad] = useState<Set<number>>(new Set());
  const [selectedLineup, setSelectedLineup] = useState<Set<number>>(new Set());
  const [liveStats, setLiveStats] = useState<LiveStat[]>([]);
  const [teamScore, setTeamScore] = useState(0);
  const [opponentScore, setOpponentScore] = useState(0);
  const [opponentName, setOpponentName] = useState('Rival');

  const applyDetail = useCallback((detail: MatchDetail) => {
    setMatch(detail);
    setSelectedSquad(new Set(detail.squad.map((p) => p.id)));
    setSelectedLineup(new Set(detail.lineup.map((p) => p.id)));
    setLiveStats(detail.liveStats);
    setTeamScore(detail.teamScore ?? 0);
    setOpponentScore(detail.opponentScore ?? 0);
    setOpponentName(detail.opponentName ?? 'Rival');
  }, []);

  const load = useCallback(async () => {
    if (!matchId) return;
    setLoading(true);
    setError(null);
    try {
      const detail = await api.get<MatchDetail>(endpoints.match(matchId));
      const teams = await api.get<Team[]>(endpoints.teams);
      const team = teams.find((t) => t.id != null && detail.teamId != null && t.id === detail.teamId);
      setTeamName(team?.name ?? 'Equipo');
      const players = await api.get<Player[]>(endpoints.players);
      const roster = detail.teamId
        ? players.filter((p) => p.team_id === String(detail.teamId))
        : players;
      applyDetail(detail);
      setAllPlayers(roster);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo cargar el partido');
    } finally {
      setLoading(false);
    }
  }, [matchId, applyDetail]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (match?.status === 'FINISHED') {
      setTab('summary');
    }
  }, [match?.status]);

  const finished = match?.status === 'FINISHED';
  const canEdit = canManageMatches && !finished;

  function toggleSquad(playerId: number) {
    if (!canEdit) return;
    setSelectedSquad((prev) => {
      const next = new Set(prev);
      if (next.has(playerId)) {
        next.delete(playerId);
        setSelectedLineup((lu) => {
          const n = new Set(lu);
          n.delete(playerId);
          return n;
        });
      } else {
        next.add(playerId);
      }
      return next;
    });
  }

  function toggleLineup(playerId: number) {
    if (!canEdit) return;
    if (!selectedSquad.has(playerId)) return;
    setSelectedLineup((prev) => {
      const next = new Set(prev);
      if (next.has(playerId)) {
        next.delete(playerId);
      } else if (next.size < 11) {
        next.add(playerId);
      }
      return next;
    });
  }

  async function saveSquad() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = await api.put<MatchDetail>(endpoints.matchSquad(matchId), {
        playerIds: [...selectedSquad],
      });
      applyDetail(detail);
      setSuccess('Convocatoria guardada');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al guardar');
    } finally {
      setSaving(false);
    }
  }

  async function saveLineup() {
    if (selectedLineup.size > 11) {
      setError('Máximo 11 jugadores en el once inicial');
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = await api.put<MatchDetail>(endpoints.matchLineup(matchId), {
        playerIds: [...selectedLineup],
      });
      applyDetail(detail);
      setSuccess('Once inicial guardado');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al guardar');
    } finally {
      setSaving(false);
    }
  }

  function patchStat(playerId: number, field: 'goals' | 'assists' | 'minutesPlayed', delta: number) {
    if (!canEdit) return;
    setLiveStats((prev) =>
      prev.map((s) => {
        if (s.playerId !== playerId) return s;
        const value = s[field] + delta;
        const clamped =
          field === 'minutesPlayed'
            ? Math.min(120, Math.max(0, value))
            : Math.max(0, value);
        return { ...s, [field]: clamped };
      }),
    );
  }

  function patchScore(field: 'teamScore' | 'opponentScore', delta: number) {
    if (!canEdit) return;
    if (field === 'teamScore') {
      setTeamScore((v) => Math.max(0, v + delta));
    } else {
      setOpponentScore((v) => Math.max(0, v + delta));
    }
  }

  async function saveScore() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const body: MatchScoreUpdate = {
        teamScore,
        opponentScore,
        opponentName: opponentName.trim() || 'Rival',
      };
      const detail = await api.put<MatchDetail>(endpoints.matchScore(matchId), body);
      applyDetail(detail);
      setSuccess('Marcador actualizado');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al guardar marcador');
    } finally {
      setSaving(false);
    }
  }

  async function closeMatch() {
    if (!confirm('¿Cerrar este partido? Ya no se podrá editar convocatoria, marcador ni estadísticas.')) return;
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = await api.put<MatchDetail>(endpoints.matchClose(matchId), {});
      applyDetail(detail);
      setSuccess('Partido cerrado');
      setTab('summary');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo cerrar el partido');
    } finally {
      setSaving(false);
    }
  }

  async function saveLiveStats() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const updated = await api.put<LiveStat[]>(endpoints.matchLiveStats(matchId), {
        stats: liveStats,
      });
      setLiveStats(updated);
      setSuccess('Estadísticas actualizadas');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al guardar estadísticas');
    } finally {
      setSaving(false);
    }
  }

  const squadPlayers = allPlayers.filter((p) => p.id != null);
  const convocated = squadPlayers.filter((p) => selectedSquad.has(p.id!));

  const tabs = finished
    ? (isUser
        ? [['summary', 'Estadísticas', BarChart3] as const]
        : ([
            ['summary', 'Estadísticas', BarChart3],
            ['squad', 'Convocatoria', Users],
            ['lineup', '11 inicial', Shirt],
            ['live', 'En vivo', Plus],
          ] as const))
    : (isUser
        ? [['live', 'En vivo', Plus] as const]
        : ([
            ['squad', 'Convocatoria', Users],
            ['lineup', '11 inicial', Shirt],
            ['live', 'En vivo', Plus],
          ] as const));

  if (loading) {
    return (
      <div className="page__center">
        <Spinner />
      </div>
    );
  }

  if (!match) {
    return <Alert>{error ?? 'Partido no encontrado'}</Alert>;
  }

  const isHome = match.home ?? true;
  const homeTeam = isHome ? teamName : opponentName;
  const awayTeam = isHome ? opponentName : teamName;
  const homeScore = isHome ? teamScore : opponentScore;
  const awayScore = isHome ? opponentScore : teamScore;
  const resultLabel =
    teamScore === opponentScore ? 'Empate' : teamScore > opponentScore ? 'Victoria' : 'Derrota';

  return (
    <section className="page match-detail">
      <header className="match-detail__header">
        <Link to="/partidos" className="match-detail__back">
          <ArrowLeft size={18} /> Partidos
        </Link>
        <div>
          <h2>{match.location}</h2>
          <p className="muted">
            {new Date(match.date).toLocaleString('es-ES', {
              dateStyle: 'full',
              timeStyle: 'short',
            })}
          </p>
        </div>
        {!isUser && (
          <div className="match-detail__counts">
            <span>{selectedSquad.size} convocados</span>
            <span>{selectedLineup.size}/11 titulares</span>
          </div>
        )}
        {canManageMatches && (
          <div className="match-detail__actions">
            {!finished ? (
              <Button onClick={() => void closeMatch()} disabled={saving}>
                <Check size={16} /> Cerrar partido
              </Button>
            ) : (
              <Badge variant="success">Finalizado</Badge>
            )}
          </div>
        )}
      </header>

      <Card
        title={finished ? 'Resultado final' : 'Marcador en vivo'}
        subtitle={finished ? 'Resultado del encuentro' : isUser ? 'Resultado del encuentro' : 'Gestiona el resultado del partido'}
      >
        {canEdit && (
          <Input label="Rival" value={opponentName} onChange={(e) => setOpponentName(e.target.value)} />
        )}
        <div className="scoreboard">
          <div className="scoreboard__team">
            <span className="scoreboard__label">{homeTeam}</span>
            <span className="scoreboard__value">{homeScore}</span>
            {canEdit && (
              <div className="scoreboard__controls">
                <button
                  type="button"
                  onClick={() => patchScore(isHome ? 'teamScore' : 'opponentScore', -1)}
                  disabled={homeScore <= 0}
                >
                  <Minus size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => patchScore(isHome ? 'teamScore' : 'opponentScore', 1)}
                >
                  <Plus size={16} />
                </button>
              </div>
            )}
          </div>
          <span className="scoreboard__sep">—</span>
          <div className="scoreboard__team scoreboard__team--away">
            <span className="scoreboard__label">{awayTeam}</span>
            <span className="scoreboard__value">{awayScore}</span>
            {canEdit && (
              <div className="scoreboard__controls">
                <button
                  type="button"
                  onClick={() => patchScore(isHome ? 'opponentScore' : 'teamScore', -1)}
                  disabled={awayScore <= 0}
                >
                  <Minus size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => patchScore(isHome ? 'opponentScore' : 'teamScore', 1)}
                >
                  <Plus size={16} />
                </button>
              </div>
            )}
          </div>
        </div>
        {finished && <Alert variant="info">{resultLabel}</Alert>}
        {canEdit && (
          <div className="match-detail__actions">
            <Button onClick={() => void saveScore()} disabled={saving}>
              <Save size={16} /> Guardar marcador
            </Button>
          </div>
        )}
      </Card>

      {isUser && (
        <Alert variant="info">
          Modo consulta: puedes ver el marcador y las estadísticas en vivo. La edición está reservada a entrenadores.
        </Alert>
      )}

      {!canManageMatches && !isUser && (
        <Alert variant="info">
          Solo entrenadores y administradores pueden editar convocatoria y estadísticas.
        </Alert>
      )}

      {error && <Alert>{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}

      <nav className="match-tabs">
        {tabs.map(([key, label, Icon]) => (
          <button
            key={key}
            type="button"
            className={`match-tabs__btn${tab === key ? ' match-tabs__btn--active' : ''}`}
            onClick={() => setTab(key)}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </nav>

      {tab === 'summary' && (
        <Card title="Estadísticas del partido" subtitle="Resumen por jugador y resultado final">
          <div className="scoreboard scoreboard--compact">
            <div className="scoreboard__team">
              <span className="scoreboard__label">{homeTeam}</span>
              <span className="scoreboard__value scoreboard__value--inline">{homeScore}</span>
            </div>
            <span className="scoreboard__sep">—</span>
            <div className="scoreboard__team scoreboard__team--away">
              <span className="scoreboard__label">{awayTeam}</span>
              <span className="scoreboard__value scoreboard__value--inline">{awayScore}</span>
            </div>
          </div>
          <Alert variant="info">{resultLabel}</Alert>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Jugador</th>
                  <th>Goles</th>
                  <th>Asist.</th>
                  <th>Min</th>
                </tr>
              </thead>
              <tbody>
                {[...liveStats]
                  .sort(
                    (a, b) =>
                      Number(b.starter) - Number(a.starter) ||
                      b.goals - a.goals ||
                      b.assists - a.assists ||
                      a.playerName.localeCompare(b.playerName),
                  )
                  .map((s) => (
                    <tr key={s.playerId}>
                      <td>
                        <strong>{s.dorsal}</strong> {s.playerName}
                        {s.starter && <span className="muted"> · Titular</span>}
                      </td>
                      <td>{s.goals}</td>
                      <td>{s.assists}</td>
                      <td>{s.minutesPlayed}</td>
                    </tr>
                  ))}
                {liveStats.length > 0 && (
                  <tr>
                    <td>
                      <strong>Total</strong>
                    </td>
                    <td>{liveStats.reduce((sum, s) => sum + s.goals, 0)}</td>
                    <td>{liveStats.reduce((sum, s) => sum + s.assists, 0)}</td>
                    <td>{liveStats.reduce((sum, s) => sum + s.minutesPlayed, 0)}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {tab === 'squad' && !isUser && (
        <Card title="Convocatoria" subtitle="Selecciona los jugadores disponibles para el partido">
          <div className="player-pick-grid">
            {squadPlayers.map((p) => {
              const selected = selectedSquad.has(p.id!);
              return (
                <button
                  key={p.id}
                  type="button"
                  disabled={!canEdit}
                  className={`player-pick${selected ? ' player-pick--on' : ''}`}
                  onClick={() => toggleSquad(p.id!)}
                >
                  <span className="player-pick__dorsal">{p.dorsal}</span>
                  <span className="player-pick__name">{p.name}</span>
                  <span className="player-pick__pos">{p.position}</span>
                  {selected && <Check size={14} className="player-pick__check" />}
                </button>
              );
            })}
          </div>
          {canEdit && (
            <div className="match-detail__actions">
              <Button onClick={() => void saveSquad()} disabled={saving}>
                <Save size={16} /> Guardar convocatoria ({selectedSquad.size})
              </Button>
            </div>
          )}
        </Card>
      )}

      {tab === 'lineup' && !isUser && (
        <Card title="Once inicial" subtitle={`${selectedLineup.size} de 11 · solo jugadores convocados`}>
          {selectedSquad.size === 0 ? (
            <p className="muted">Primero guarda la convocatoria en la pestaña anterior.</p>
          ) : (
            <>
              <div className="pitch" aria-label="Campo de fútbol">
                {convocated
                  .filter((p) => selectedLineup.has(p.id!))
                  .map((p) => (
                    <div key={p.id} className="pitch__player" title={p.name}>
                      <span>{p.dorsal}</span>
                    </div>
                  ))}
                {selectedLineup.size === 0 && (
                  <p className="pitch__empty">Toca jugadores abajo para el 11</p>
                )}
              </div>
              <div className="player-pick-grid">
                {convocated.map((p) => {
                  const selected = selectedLineup.has(p.id!);
                  const full = selectedLineup.size >= 11 && !selected;
                  return (
                    <button
                      key={p.id}
                      type="button"
                      disabled={!canEdit || full}
                      className={`player-pick${selected ? ' player-pick--starter' : ''}${full ? ' player-pick--disabled' : ''}`}
                      onClick={() => toggleLineup(p.id!)}
                    >
                      <span className="player-pick__dorsal">{p.dorsal}</span>
                      <span className="player-pick__name">{p.name}</span>
                      <span className="player-pick__pos">{p.position}</span>
                    </button>
                  );
                })}
              </div>
              {canEdit && (
                <div className="match-detail__actions">
                  <Button onClick={() => void saveLineup()} disabled={saving || selectedLineup.size === 0}>
                    <Save size={16} /> Guardar once ({selectedLineup.size}/11)
                  </Button>
                </div>
              )}
            </>
          )}
        </Card>
      )}

      {tab === 'live' && (
        <Card title="Estadísticas en vivo" subtitle="Goles, asistencias y minutos por jugador">
          {liveStats.length === 0 ? (
            <p className="muted">
              {isUser
                ? 'Aún no hay estadísticas para este partido.'
                : 'Guarda la convocatoria para habilitar estadísticas.'}
            </p>
          ) : (
            <>
              <ul className="live-stats">
                {liveStats.map((stat) => (
                  <li key={stat.playerId} className="live-stat">
                    <div className="live-stat__player">
                      <span className="live-stat__dorsal">{stat.dorsal}</span>
                      <div>
                        <strong>{stat.playerName}</strong>
                        <span className="muted">{stat.position}</span>
                        {stat.starter && <Badge>Titular</Badge>}
                      </div>
                    </div>
                    <StatControl
                      label="Goles"
                      value={stat.goals}
                      onDec={() => patchStat(stat.playerId, 'goals', -1)}
                      onInc={() => patchStat(stat.playerId, 'goals', 1)}
                      disabled={!canEdit}
                    />
                    <StatControl
                      label="Asist."
                      value={stat.assists}
                      onDec={() => patchStat(stat.playerId, 'assists', -1)}
                      onInc={() => patchStat(stat.playerId, 'assists', 1)}
                      disabled={!canEdit}
                    />
                    <StatControl
                      label="Min"
                      value={stat.minutesPlayed}
                      onDec={() => patchStat(stat.playerId, 'minutesPlayed', -1)}
                      onInc={() => patchStat(stat.playerId, 'minutesPlayed', 1)}
                      disabled={!canEdit}
                    />
                  </li>
                ))}
              </ul>
              {canEdit && (
                <div className="match-detail__actions">
                  <Button onClick={() => void saveLiveStats()} disabled={saving}>
                    <Save size={16} /> Guardar estadísticas
                  </Button>
                </div>
              )}
            </>
          )}
        </Card>
      )}
    </section>
  );
}

function StatControl({
  label,
  value,
  onDec,
  onInc,
  disabled,
}: {
  label: string;
  value: number;
  onDec: () => void;
  onInc: () => void;
  disabled?: boolean;
}) {
  return (
    <div className="stat-control">
      <span className="stat-control__label">{label}</span>
      <div className="stat-control__buttons">
        <button type="button" onClick={onDec} disabled={disabled || value <= 0} aria-label={`Menos ${label}`}>
          <Minus size={14} />
        </button>
        <span className="stat-control__value">{value}</span>
        <button type="button" onClick={onInc} disabled={disabled} aria-label={`Más ${label}`}>
          <Plus size={14} />
        </button>
      </div>
    </div>
  );
}
