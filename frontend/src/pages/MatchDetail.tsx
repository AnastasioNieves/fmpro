import { useCallback, useEffect, useState, useRef } from 'react';
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
  Play,
  Pause,
  Square,
  ArrowRightLeft,
  Clock,
} from 'lucide-react';
import { api, ApiError, endpoints } from '../api/client';

function getPositionWeight(position: string): number {
  if (!position) return 5;
  const p = position.toUpperCase();
  if (['POR', 'PORTERO'].some(r => p.includes(r))) return 1;
  if (['DEF', 'LAT', 'DFC', 'LTI', 'LTD', 'CARRILERO'].some(r => p.includes(r))) return 2;
  if (['MED', 'MC', 'MCD', 'MCO', 'MI', 'MD', 'PIVOTE', 'INTERIOR', 'CENTRO'].some(r => p.includes(r))) return 3;
  if (['DEL', 'EX', 'DC', 'EI', 'ED', 'PUNTA'].some(r => p.includes(r))) return 4;
  return 5;
}
import { useAuth } from '../context/AuthContext';
import { Alert, Badge, Button, Card, Input, Spinner, Select } from '../components/ui';
import type { LiveStat, MatchDetail, MatchScoreUpdate, Player, Team } from '../types';

type Tab = 'summary' | 'squad' | 'lineup' | 'live';

export function MatchDetail() {
  const { id } = useParams<{ id: string }>();
  const matchId = id || '';
  const { canManageMatches, isUser } = useAuth();

  const [match, setMatch] = useState<MatchDetail | null>(null);
  const [teamName, setTeamName] = useState('Equipo');
  const [allPlayers, setAllPlayers] = useState<Player[]>([]);
  const [tab, setTab] = useState<Tab>(isUser ? 'live' : 'squad');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [selectedSquad, setSelectedSquad] = useState<Set<string>>(new Set());
  const [selectedLineup, setSelectedLineup] = useState<Set<string>>(new Set());
  const [liveStats, setLiveStats] = useState<LiveStat[]>([]);
  const [teamScore, setTeamScore] = useState(0);
  const [opponentScore, setOpponentScore] = useState(0);
  const [opponentName, setOpponentName] = useState('Rival');
  
  // Substitution State
  const [subModalOpen, setSubModalOpen] = useState(false);
  const [subPlayerOut, setSubPlayerOut] = useState<string | null>(null);
  const [subPlayerIn, setSubPlayerIn] = useState<string | null>(null);
  const [formation, setFormation] = useState<import('../types').FormationType>('4-3-3');
  const [lineupPositions, setLineupPositions] = useState<Record<string, string>>({});

  // Timer State
  const [timerState, setTimerState] = useState<{ isRunning: boolean; startTime: number | null; elapsedSeconds: number }>({ isRunning: false, startTime: null, elapsedSeconds: 0 });
  const [liveElapsed, setLiveElapsed] = useState(0);
  const selectedLineupRef = useRef(selectedLineup);
  const lastMinuteRef = useRef(0);

  useEffect(() => {
    selectedLineupRef.current = selectedLineup;
  }, [selectedLineup]);
  const formationSlots: Record<string, string[]> = {
    '4-3-3': ['POR', 'LTI', 'DFC1', 'DFC2', 'LTD', 'MCI', 'MCD', 'MCD_R', 'EI', 'DC', 'ED'],
    '4-4-2': ['POR', 'LTI', 'DFC1', 'DFC2', 'LTD', 'MI', 'MCI', 'MCD_R', 'MD', 'DC1', 'DC2'],
    '3-5-2': ['POR', 'DFC1', 'DFC3', 'DFC2', 'CARI', 'MCI', 'MCD_R', 'MCD', 'CARD', 'DC1', 'DC2'],
    '4-2-3-1': ['POR', 'LTI', 'DFC1', 'DFC2', 'LTD', 'MCD1', 'MCD2', 'MI', 'MCO', 'MD', 'DC'],
    'Personalizada': ['POR', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG', 'JUG'],
  };

  const positionCoordinates: Record<string, { top: string; left: string }> = {
    'POR': { top: '88%', left: '50%' },
    'DFC1': { top: '75%', left: '35%' },
    'DFC2': { top: '75%', left: '65%' },
    'DFC3': { top: '75%', left: '50%' },
    'LTI': { top: '70%', left: '15%' },
    'LTD': { top: '70%', left: '85%' },
    'CARI': { top: '65%', left: '15%' },
    'CARD': { top: '65%', left: '85%' },
    'MCD': { top: '58%', left: '50%' },
    'MCD1': { top: '58%', left: '35%' },
    'MCD2': { top: '58%', left: '65%' },
    'MC': { top: '45%', left: '50%' },
    'MCI': { top: '45%', left: '30%' },
    'MCD_R': { top: '45%', left: '70%' },
    'MCO': { top: '32%', left: '50%' },
    'MI': { top: '40%', left: '15%' },
    'MD': { top: '40%', left: '85%' },
    'EI': { top: '25%', left: '20%' },
    'ED': { top: '25%', left: '80%' },
    'DC': { top: '15%', left: '50%' },
    'DC1': { top: '15%', left: '35%' },
    'DC2': { top: '15%', left: '65%' },
    'JUG': { top: '50%', left: '50%' },
  };

  const applyDetail = useCallback((detail: MatchDetail) => {
    setMatch(detail);
    setSelectedSquad(new Set(detail.squad.map((p) => String(p.id))));
    setSelectedLineup(new Set(detail.lineup.map((p) => String(p.id))));
    setLiveStats(detail.liveStats);
    setTeamScore(detail.teamScore ?? 0);
    setOpponentScore(detail.opponentScore ?? 0);
    setOpponentName(detail.opponentName ?? 'Rival');

    // Recuperar tácticas y tiempo locales temporalmente
    const storedData = localStorage.getItem(`match-${detail.id}-tactics`);
    if (storedData) {
      try {
        const { f, lp } = JSON.parse(storedData);
        if (f) setFormation(f);
        if (lp) setLineupPositions(lp);
      } catch (e) {}
    }
    const storedTimer = localStorage.getItem(`match-${detail.id}-timer`);
    if (storedTimer) {
      try {
        const parsed = JSON.parse(storedTimer);
        setTimerState(parsed);
        setLiveElapsed(parsed.elapsedSeconds);
        lastMinuteRef.current = Math.floor(parsed.elapsedSeconds / 60);
      } catch (e) {}
    }
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
    if (!matchId) return;
    localStorage.setItem(`match-${matchId}-timer`, JSON.stringify(timerState));
  }, [timerState, matchId]);

  useEffect(() => {
    if (!timerState.isRunning || !timerState.startTime) {
      lastMinuteRef.current = Math.floor(timerState.elapsedSeconds / 60);
      return;
    }
    const interval = setInterval(() => {
      const elapsed = timerState.elapsedSeconds + Math.floor((Date.now() - timerState.startTime!) / 1000);
      setLiveElapsed(elapsed);
      
      const currentMinute = Math.floor(elapsed / 60);
      if (currentMinute > lastMinuteRef.current) {
        const diff = currentMinute - lastMinuteRef.current;
        lastMinuteRef.current = currentMinute;
        
        setLiveStats(prev => prev.map(s => {
           if (selectedLineupRef.current.has(s.playerId)) {
             return { ...s, minutesPlayed: (s.minutesPlayed || 0) + diff };
           }
           return s;
        }));
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [timerState]);

  useEffect(() => {
    if (match?.status === 'FINISHED') {
      setTab('summary');
    }
  }, [match?.status]);

  const finished = match?.status === 'FINISHED';
  const canEdit = canManageMatches && !finished;

  function toggleSquad(playerId: string) {
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

  function toggleLineup(playerId: string) {
    if (!canEdit) return;
    if (!selectedSquad.has(playerId)) return;
    setSelectedLineup((prev) => {
      const next = new Set(prev);
      if (next.has(playerId)) {
        next.delete(playerId);
        setLineupPositions((lp) => {
          const newLp = { ...lp };
          delete newLp[playerId];
          return newLp;
        });
      } else if (next.size < 11) {
        next.add(playerId);
        // Assign to first empty slot
        const slots = formationSlots[formation] || formationSlots['4-3-3'];
        // Find a slot index that isn't taken, just loosely (not robust for same roles, but works as placeholder)
        // Better: just assign a generic role for now, we'll refine the UI.
        const defaultRole = slots[next.size - 1] || 'JUG';
        setLineupPositions((lp) => ({ ...lp, [playerId]: defaultRole }));
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
      // Guardar formación localmente
      localStorage.setItem(`match-${matchId}-tactics`, JSON.stringify({ f: formation, lp: lineupPositions }));
      applyDetail(detail);
      setSuccess('Once inicial y táctica guardada');
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Error al guardar');
    } finally {
      setSaving(false);
    }
  }

  function patchStat(playerId: string, field: string, delta: number) {
    if (!canEdit) return;
    setLiveStats((prev) =>
      prev.map((s) => {
        if (s.playerId !== playerId) return s;
        const currentValue = (s as any)[field] || 0;
        const value = Math.max(0, Math.round((currentValue + delta) * 100) / 100);
        const newS: any = { ...s, [field]: value };
        
        // Auto-suma logic (solo si estamos incrementando)
        if (delta > 0) {
          if (field === 'shotsOnTarget') newS.shotsTotal = Math.max(newS.shotsTotal || 0, value);
          if (field === 'passesCompleted') newS.passesTotal = Math.max(newS.passesTotal || 0, value);
          if (field === 'duelsWon') newS.duelsTotal = Math.max(newS.duelsTotal || 0, value);
          if (field === 'goals') {
             newS.shotsOnTarget = Math.max(newS.shotsOnTarget || 0, value);
             newS.shotsTotal = Math.max(newS.shotsTotal || 0, newS.shotsOnTarget);
          }
        }
        
        // Handling minutesPlayed clamping
        if (field === 'minutesPlayed') {
          newS.minutesPlayed = Math.min(120, Math.max(0, value));
        }
        
        return newS;
      })
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

  function toggleTimer() {
    if (!canEdit) return;
    setTimerState(prev => {
      if (prev.isRunning) {
        const currentElapsed = prev.elapsedSeconds + Math.floor((Date.now() - prev.startTime!) / 1000);
        return { isRunning: false, startTime: null, elapsedSeconds: currentElapsed };
      } else {
        return { isRunning: true, startTime: Date.now(), elapsedSeconds: prev.elapsedSeconds };
      }
    });
  }

  function resetTimer() {
    if (!canEdit) return;
    if (confirm('¿Reiniciar cronómetro y minutos? (Nota: los minutos de los jugadores no se borrarán automáticamente)')) {
      setTimerState({ isRunning: false, startTime: null, elapsedSeconds: 0 });
      setLiveElapsed(0);
      lastMinuteRef.current = 0;
    }
  }

  function handleSubstitution() {
    if (!subPlayerOut || !subPlayerIn) return;
    
    setSelectedLineup(prev => {
      const next = new Set(prev);
      next.delete(subPlayerOut);
      next.add(subPlayerIn);
      
      setLineupPositions(lp => {
         const newLp = { ...lp };
         const roleOut = newLp[subPlayerOut] || convocated.find(p => p.id === subPlayerOut)?.position || 'JUG';
         newLp[subPlayerIn] = roleOut;
         localStorage.setItem(`match-${matchId}-tactics`, JSON.stringify({ f: formation, lp: newLp }));
         return newLp;
      });
      
      return next;
    });
    setSubModalOpen(false);
    setSubPlayerOut(null);
    setSubPlayerIn(null);
  }

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const squadPlayers = allPlayers.filter((p) => p.id != null);
  const convocated = squadPlayers.filter((p) => selectedSquad.has(String(p.id)));

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
              const selected = selectedSquad.has(String(p.id));
              return (
                <button
                  key={p.id}
                  type="button"
                  disabled={!canEdit}
                  className={`player-pick${selected ? ' player-pick--on' : ''}`}
                  onClick={() => toggleSquad(String(p.id))}
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
        <Card title="Pizarra Táctica (Once Inicial)" subtitle={`${selectedLineup.size} de 11 · Configura la formación y posiciones`}>
          {selectedSquad.size === 0 ? (
            <p className="muted">Primero guarda la convocatoria en la pestaña anterior.</p>
          ) : (
            <>
              <div style={{ marginBottom: '1.5rem' }}>
                <Select
                  label="Formación"
                  value={formation}
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    setFormation(e.target.value as any);
                    setLineupPositions({});
                    setSelectedLineup(new Set());
                  }}
                  disabled={!canEdit}
                >
                  <option value="4-3-3">4-3-3</option>
                  <option value="4-4-2">4-4-2</option>
                  <option value="3-5-2">3-5-2</option>
                  <option value="4-2-3-1">4-2-3-1</option>
                  <option value="Personalizada">Personalizada (Libre)</option>
                </Select>
              </div>
              <div className="pitch-glass" aria-label="Campo de táctica avanzada">
                {/* Dibujo del campo (líneas verdes transparentes) */}
                <div className="pitch-lines">
                  <div className="pitch-box-top"></div>
                  <div className="pitch-box-bottom"></div>
                  <div className="pitch-circle"></div>
                  <div className="pitch-halfway"></div>
                </div>
                
                {(() => {
                  const slotsForFormation = formationSlots[formation] || formationSlots['4-3-3'];
                  const assignedPlayers = new Map<string, string>();
                  const takenSlots = new Set<string>();
                  
                  const selectedPlayersList = convocated.filter((p) => selectedLineup.has(String(p.id)));
                  
                  // Primera pasada: respetar posiciones guardadas si existen en la formación actual
                  selectedPlayersList.forEach(p => {
                    const pref = lineupPositions[String(p.id)];
                    if (pref && slotsForFormation.includes(pref) && !takenSlots.has(pref)) {
                      assignedPlayers.set(String(p.id), pref);
                      takenSlots.add(pref);
                    }
                  });
                  
                  // Segunda pasada: rellenar los huecos libres con los jugadores restantes
                  const availableSlots = slotsForFormation.filter(s => !takenSlots.has(s));
                  let slotIdx = 0;
                  
                  selectedPlayersList.forEach(p => {
                    if (!assignedPlayers.has(String(p.id))) {
                      const fallbackSlot = availableSlots[slotIdx] || 'JUG';
                      assignedPlayers.set(String(p.id), fallbackSlot);
                      slotIdx++;
                    }
                  });

                  return selectedPlayersList.map((p) => {
                    const role = assignedPlayers.get(String(p.id)) || 'JUG';
                    const coords = positionCoordinates[role] || positionCoordinates['JUG'];
                    
                    return (
                      <div key={p.id} className="pitch__player" title={p.name} style={{ position: 'absolute', top: coords.top, left: coords.left, transform: 'translate(-50%, -50%)' }}>
                        <span>{p.dorsal}</span>
                        <div className="player-role">
                          {role}
                        </div>
                      </div>
                    );
                  });
                })()}
                {selectedLineup.size === 0 && (
                  <p className="pitch__empty" style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 10 }}>Selecciona abajo a los 11 titulares</p>
                )}
              </div>
              <div className="player-pick-grid" style={{ marginTop: '1rem' }}>
                {convocated.map((p) => {
                  const selected = selectedLineup.has(String(p.id));
                  const full = selectedLineup.size >= 11 && !selected;
                  return (
                    <button
                      key={p.id}
                      type="button"
                      disabled={!canEdit || full}
                      className={`player-pick${selected ? ' player-pick--starter' : ''}${full ? ' player-pick--disabled' : ''}`}
                      onClick={() => toggleLineup(String(p.id))}
                    >
                      <span className="player-pick__dorsal">{p.dorsal}</span>
                      <span className="player-pick__name">{p.name}</span>
                      <span className="player-pick__pos">{selected ? lineupPositions[String(p.id)] : p.position}</span>
                    </button>
                  );
                })}
              </div>
              {canEdit && (
                <div className="match-detail__actions">
                  <Button onClick={() => void saveLineup()} disabled={saving || selectedLineup.size === 0}>
                    <Save size={16} /> Guardar táctica ({selectedLineup.size}/11)
                  </Button>
                </div>
              )}
            </>
          )}
        </Card>
      )}

      {tab === 'live' && (
        <div className="live-dashboard">
          {/* Action Bar (Opción 1) */}
          <div className="match-timer-bar">
            <div className="match-timer-bar__clock">
              <Clock size={24} className={timerState.isRunning ? 'text-accent spin-slow' : 'text-muted'} />
              <span className={`match-timer-bar__time ${timerState.isRunning ? 'glow' : ''}`}>
                {formatTime(liveElapsed)}
              </span>
            </div>
            {canEdit && (
              <div className="match-timer-bar__controls">
                <button type="button" onClick={toggleTimer} className={`btn-icon ${timerState.isRunning ? 'btn-icon--pause' : 'btn-icon--play'}`}>
                  {timerState.isRunning ? <Pause size={18} /> : <Play size={18} />}
                </button>
                <button type="button" onClick={resetTimer} className="btn-icon btn-icon--stop">
                  <Square size={18} />
                </button>
                <button type="button" onClick={() => setSubModalOpen(true)} className="btn-action">
                  <ArrowRightLeft size={16} /> Hacer Cambio
                </button>
              </div>
            )}
          </div>

          {/* Modal de Sustitución */}
          {subModalOpen && (
            <div className="modal-overlay">
              <div className="modal-content">
                <h3>Realizar Sustitución</h3>
                <div className="sub-grid">
                  <div className="sub-col">
                    <label className="text-rose">Sale del campo (Rojo)</label>
                    <Select value={subPlayerOut || ''} onChange={e => setSubPlayerOut(e.target.value)}>
                      <option value="">Seleccionar jugador...</option>
                      {convocated.filter(p => selectedLineup.has(String(p.id))).map(p => (
                        <option key={p.id} value={String(p.id)}>{p.dorsal} - {p.name}</option>
                      ))}
                    </Select>
                  </div>
                  <div className="sub-col">
                    <label className="text-accent">Entra al campo (Verde)</label>
                    <Select value={subPlayerIn || ''} onChange={e => setSubPlayerIn(e.target.value)}>
                      <option value="">Seleccionar jugador...</option>
                      {convocated.filter(p => !selectedLineup.has(String(p.id))).map(p => (
                        <option key={p.id} value={String(p.id)}>{p.dorsal} - {p.name}</option>
                      ))}
                    </Select>
                  </div>
                </div>
                <div className="modal-actions">
                  <Button variant="secondary" onClick={() => setSubModalOpen(false)}>Cancelar</Button>
                  <Button onClick={handleSubstitution} disabled={!subPlayerOut || !subPlayerIn}>Confirmar Cambio</Button>
                </div>
              </div>
            </div>
          )}

          <Card title="Estadísticas en vivo" subtitle="Goles, asistencias y minutos por jugador">
            {liveStats.length === 0 ? (
              <p className="muted">
                {isUser
                  ? 'Aún no hay estadísticas para este partido.'
                  : 'Guarda la convocatoria para habilitar estadísticas.'}
              </p>
            ) : (
              <>
                <h3 className="section-title text-accent">En el Campo (Titulares)</h3>
                <ul className="live-stats" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', listStyle: 'none', padding: 0, marginBottom: '2rem' }}>
                  {[...liveStats]
                    .filter(s => selectedLineup.has(s.playerId))
                    .sort((a, b) => getPositionWeight(lineupPositions[a.playerId] || a.position) - getPositionWeight(lineupPositions[b.playerId] || b.position))
                    .map((stat) => (
                    <li key={stat.playerId} className="live-stat-glass">
                      <div className="live-stat__main" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', flexWrap: 'wrap', gap: '1rem' }}>
                        <div className="live-stat__player" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                          <span className="live-stat__dorsal" style={{ width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--surface-sunken)', borderRadius: '12px', fontWeight: '800', fontSize: '1.1rem', color: 'var(--text)' }}>{stat.dorsal}</span>
                          <div>
                            <strong style={{ display: 'block', fontSize: '1.1rem', marginBottom: '0.1rem' }}>{stat.playerName}</strong>
                            <span className="muted" style={{ fontSize: '0.85rem' }}>{lineupPositions[stat.playerId] || stat.position}</span>
                            <span style={{ marginLeft: '0.5rem' }}><Badge variant="success">Jugando</Badge></span>
                          </div>
                        </div>
                        <div className="live-stat__controls" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', padding: '0.5rem', background: 'var(--surface-sunken)', borderRadius: '12px' }}>
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
                            label="Minutos"
                            value={stat.minutesPlayed ?? 0}
                            onDec={() => patchStat(stat.playerId, 'minutesPlayed', -1)}
                            onInc={() => patchStat(stat.playerId, 'minutesPlayed', 1)}
                            disabled={!canEdit}
                          />
                        </div>
                      </div>
                      
                      <div className="live-stat-glass__advanced">
                        {/* Filtramos qué controles renderizar según el rol del jugador */}
                        {(() => {
                          const isFwd = ['DEL', 'EX', 'DC', 'EI', 'ED', 'Delantero'].some(r => (lineupPositions[stat.playerId] || stat.position)?.toUpperCase().includes(r));
                          const isMid = ['MED', 'MC', 'MCD', 'MCO', 'MI', 'MD', 'Centrocampista'].some(r => (lineupPositions[stat.playerId] || stat.position)?.toUpperCase().includes(r));
                          const isDef = ['DEF', 'LAT', 'DFC', 'LTI', 'LTD', 'Defensa'].some(r => (lineupPositions[stat.playerId] || stat.position)?.toUpperCase().includes(r));
                          const isGk = ['POR', 'Portero'].some(r => (lineupPositions[stat.playerId] || stat.position)?.toUpperCase().includes(r));

                          return (
                            <>
                              {(isFwd || isMid) && (
                                <>
                                  <StatControl
                                    label="Tiros Totales"
                                    value={stat.shotsTotal ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'shotsTotal', -1)}
                                    onInc={() => patchStat(stat.playerId, 'shotsTotal', 1)}
                                    disabled={!canEdit}
                                  />
                                  <StatControl
                                    label="Tiros Puerta"
                                    value={stat.shotsOnTarget ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'shotsOnTarget', -1)}
                                    onInc={() => patchStat(stat.playerId, 'shotsOnTarget', 1)}
                                    disabled={!canEdit}
                                  />
                                </>
                              )}
                              {(isMid || isFwd || isDef) && (
                                <>
                                  <StatControl
                                    label="Pases Totales"
                                    value={stat.passesTotal ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'passesTotal', -1)}
                                    onInc={() => patchStat(stat.playerId, 'passesTotal', 1)}
                                    disabled={!canEdit}
                                  />
                                  <StatControl
                                    label="Pases Complet."
                                    value={stat.passesCompleted ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'passesCompleted', -1)}
                                    onInc={() => patchStat(stat.playerId, 'passesCompleted', 1)}
                                    disabled={!canEdit}
                                  />
                                </>
                              )}
                              {(isDef || isMid || isFwd) && (
                                <>
                                  <StatControl
                                    label="Duelos Totales"
                                    value={stat.duelsTotal ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'duelsTotal', -1)}
                                    onInc={() => patchStat(stat.playerId, 'duelsTotal', 1)}
                                    disabled={!canEdit}
                                  />
                                  <StatControl
                                    label="Duelos Ganados"
                                    value={stat.duelsWon ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'duelsWon', -1)}
                                    onInc={() => patchStat(stat.playerId, 'duelsWon', 1)}
                                    disabled={!canEdit}
                                  />
                                </>
                              )}
                              {(isDef || isMid) && (
                                <StatControl
                                  label="Intercepciones"
                                  value={stat.interceptions ?? 0}
                                  onDec={() => patchStat(stat.playerId, 'interceptions', -1)}
                                  onInc={() => patchStat(stat.playerId, 'interceptions', 1)}
                                  disabled={!canEdit}
                                />
                              )}
                              {isGk && (
                                <>
                                  <StatControl
                                    label="Paradas"
                                    value={stat.saves ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'saves', -1)}
                                    onInc={() => patchStat(stat.playerId, 'saves', 1)}
                                    disabled={!canEdit}
                                  />
                                  <StatControl
                                    label="Goles Enc."
                                    value={stat.goalsConceded ?? 0}
                                    onDec={() => patchStat(stat.playerId, 'goalsConceded', -1)}
                                    onInc={() => patchStat(stat.playerId, 'goalsConceded', 1)}
                                    disabled={!canEdit}
                                  />
                                </>
                              )}
                            </>
                          );
                        })()}
                      </div>
                    </li>
                  ))}
                </ul>

                <h3 className="section-title text-muted">Banquillo y Sustituidos</h3>
                <ul className="live-stats" style={{ display: 'flex', flexDirection: 'column', gap: '1rem', listStyle: 'none', padding: 0 }}>
                  {[...liveStats]
                    .filter(s => !selectedLineup.has(s.playerId))
                    .sort((a, b) => getPositionWeight(lineupPositions[a.playerId] || a.position) - getPositionWeight(lineupPositions[b.playerId] || b.position))
                    .map((stat) => (
                    <li key={stat.playerId} className="live-stat-glass live-stat-glass--bench">
                      <div className="live-stat__main" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%', flexWrap: 'wrap', gap: '1rem' }}>
                        <div className="live-stat__player" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                          <span className="live-stat__dorsal" style={{ width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--surface-sunken)', borderRadius: '12px', fontWeight: '800', fontSize: '1.1rem', color: 'var(--text)' }}>{stat.dorsal}</span>
                          <div>
                            <strong style={{ display: 'block', fontSize: '1.1rem', marginBottom: '0.1rem' }}>{stat.playerName}</strong>
                            <span className="muted" style={{ fontSize: '0.85rem' }}>Banquillo ({stat.position})</span>
                            {(stat.minutesPlayed || 0) > 0 ? (
                               <span style={{ marginLeft: '0.5rem' }}><Badge>Sustituido</Badge></span>
                            ) : (
                               <span style={{ marginLeft: '0.5rem' }}><Badge>Banquillo</Badge></span>
                            )}
                          </div>
                        </div>
                        <div className="live-stat__controls" style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', padding: '0.5rem', background: 'var(--surface-sunken)', borderRadius: '12px' }}>
                          <StatControl label="Minutos" value={stat.minutesPlayed ?? 0} onDec={() => patchStat(stat.playerId, 'minutesPlayed', -1)} onInc={() => patchStat(stat.playerId, 'minutesPlayed', 1)} disabled={!canEdit} />
                          <StatControl label="Goles" value={stat.goals} onDec={() => patchStat(stat.playerId, 'goals', -1)} onInc={() => patchStat(stat.playerId, 'goals', 1)} disabled={!canEdit} />
                          <StatControl label="Asist." value={stat.assists} onDec={() => patchStat(stat.playerId, 'assists', -1)} onInc={() => patchStat(stat.playerId, 'assists', 1)} disabled={!canEdit} />
                        </div>
                      </div>
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
        </div>
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
    <div className="stat-control" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.2rem' }}>
      <span className="stat-control__label" style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{label}</span>
      <div className="stat-control__buttons" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
        <button type="button" onClick={onDec} disabled={disabled || value <= 0} aria-label={`Menos ${label}`} style={{ padding: '2px', background: 'var(--overlay)', border: '1px solid var(--border)', borderRadius: '4px', color: 'var(--text)' }}>
          <Minus size={12} />
        </button>
        <span className="stat-control__value" style={{ fontSize: '0.9rem', fontWeight: 600, minWidth: '1.5rem', textAlign: 'center' }}>{value}</span>
        <button type="button" onClick={onInc} disabled={disabled} aria-label={`Más ${label}`} style={{ padding: '2px', background: 'var(--overlay)', border: '1px solid var(--border)', borderRadius: '4px', color: 'var(--text)' }}>
          <Plus size={12} />
        </button>
      </div>
    </div>
  );
}
