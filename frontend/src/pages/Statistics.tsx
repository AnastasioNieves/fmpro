import { useCallback, useEffect, useMemo, useState } from 'react';
import { BarChart3, FileDown } from 'lucide-react';
import { api, ApiError, downloadBlob, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  Alert,
  Button,
  Card,
  EmptyState,
  Input,
  Select,
  Spinner,
} from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Player, Statistic, Team } from '../types';
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';

type FilterMode = 'team' | 'player';

function formatFraction(part: number | undefined, total: number | undefined) {
  const p = part ?? 0;
  const t = total ?? 0;
  if (t === 0) return <span>-</span>;
  const percent = Math.round((p / t) * 100);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', lineHeight: '1.2' }}>
      <span>{p}/{t}</span>
      <span style={{ fontSize: '0.7rem', color: percent >= 70 ? 'var(--accent)' : 'var(--text-muted)' }}>{percent}%</span>
    </div>
  );
}

function generateMockRadarData(position?: string) {
  const p = position?.toLowerCase() || '';
  const isDefender = p.includes('def') || p.includes('lateral') || p.includes('central');
  const isMidfielder = p.includes('med') || p.includes('cen') || p.includes('pivote');
  const isForward = p.includes('del') || p.includes('extremo') || p.includes('punta');
  
  return [
    { metric: 'Tiros Pta %', value: isForward ? 85 : isMidfielder ? 60 : 20, fullMark: 100 },
    { metric: 'Acierto Pase', value: isForward ? 70 : isMidfielder ? 85 : 40, fullMark: 100 },
    { metric: 'Goles/Tiro', value: isDefender ? 65 : isMidfielder ? 90 : 55, fullMark: 100 },
    { metric: 'Intercepciones', value: isDefender ? 90 : isMidfielder ? 70 : 30, fullMark: 100 },
    { metric: 'Duelos Ganados', value: isDefender ? 85 : isMidfielder ? 75 : 40, fullMark: 100 },
    { metric: 'Paradas %', value: p.includes('por') ? 80 : 0, fullMark: 100 },
  ];
}

export function Statistics() {
  const { canExportReports, user, isUser } = useAuth();
  const teams = useFetch(() => api.get<Team[]>(endpoints.teams), []);
  const players = useFetch(() => api.get<Player[]>(endpoints.players), []);

  const [filterMode, setFilterMode] = useState<FilterMode>(isUser ? 'team' : 'team');
  const [teamId, setTeamId] = useState(isUser && user?.teamId ? String(user.teamId) : '');
  const [playerId, setPlayerId] = useState('');
  const [playerQuery, setPlayerQuery] = useState('');
  const [stats, setStats] = useState<Statistic[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  const matchedPlayerId = useMemo(() => {
    if (!isUser) return null;
    const q = playerQuery.trim().toLowerCase();
    if (!q) return null;
    const pool = players.data?.filter((p) => p.team_id === teamId) ?? [];
    const exact = pool.find((p) => p.name.trim().toLowerCase() === q);
    return exact?.id != null ? String(exact.id) : null;
  }, [isUser, playerQuery, players.data, teamId]);

  const effectiveFilterMode: FilterMode = isUser ? (matchedPlayerId ? 'player' : 'team') : filterMode;
  const selectedTeamId = effectiveFilterMode === 'team' ? teamId : '';
  const selectedPlayerId = effectiveFilterMode === 'player' ? (isUser ? matchedPlayerId ?? '' : playerId) : '';
  const hasSelection =
    (effectiveFilterMode === 'team' && selectedTeamId !== '') ||
    (effectiveFilterMode === 'player' && selectedPlayerId !== '');

  const filteredPlayers =
    effectiveFilterMode === 'team' && selectedTeamId
      ? players.data?.filter((p) => p.team_id === selectedTeamId)
      : players.data;

  const selectionLabel =
    effectiveFilterMode === 'team'
      ? teams.data?.find((t) => String(t.id) === selectedTeamId)?.name
      : players.data?.find((p) => String(p.id) === selectedPlayerId)?.name;

  const selectedPlayerPosition = 
    effectiveFilterMode === 'player' 
      ? players.data?.find((p) => String(p.id) === selectedPlayerId)?.position 
      : undefined;

  const mockRadarData = useMemo(() => {
    if (effectiveFilterMode !== 'player' || !hasSelection) return [];
    return generateMockRadarData(selectedPlayerPosition);
  }, [effectiveFilterMode, hasSelection, selectedPlayerPosition]);

  const loadStats = useCallback(async () => {
    if (!hasSelection) {
      setStats(null);
      setError(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const path =
        effectiveFilterMode === 'team'
          ? endpoints.statisticsByTeam(Number(selectedTeamId))
          : endpoints.statisticsByPlayer(Number(selectedPlayerId));
      const data = await api.get<Statistic[]>(path);
      setStats(data);
    } catch (err) {
      setStats(null);
      setError(err instanceof ApiError ? err.message : 'No se pudieron cargar las estadísticas');
    } finally {
      setLoading(false);
    }
  }, [effectiveFilterMode, hasSelection, selectedTeamId, selectedPlayerId]);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  function handleModeChange(mode: FilterMode) {
    setFilterMode(mode);
    setTeamId('');
    setPlayerId('');
    setStats(null);
    setError(null);
  }

  async function handleExportPdf() {
    if (!hasSelection) return;
    setExporting(true);
    try {
      const path =
        filterMode === 'team'
          ? endpoints.reportTeam(Number(selectedTeamId))
          : endpoints.reportPlayer(Number(selectedPlayerId));
      const blob = await api.download(path);
      const slug = selectionLabel?.replace(/\s+/g, '-').toLowerCase() ?? 'informe';
      downloadBlob(blob, `informe-${filterMode === 'team' ? 'equipo' : 'jugador'}-${slug}.pdf`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo generar el PDF');
    } finally {
      setExporting(false);
    }
  }



  const totalsByPlayer = useMemo(() => {
    if (!stats || effectiveFilterMode !== 'team') return null;

    const byPlayer = new Map<
      string,
      { id: string; name: string; position: string; goals: number; assists: number; minutes: number; shotsOnTarget: number; shotsTotal: number; passesCompleted: number; passesTotal: number; duelsWon: number; duelsTotal: number; interceptions: number; saves: number; }
    >();

    for (const s of stats) {
      const id =
        s.player?.id != null
          ? String(s.player.id)
          : s.player?.name
            ? `name:${s.player.name}`
            : `unknown:${s.id ?? Math.random()}`;
      const name = s.player?.name ?? '—';
      const position = s.player?.position ?? players.data?.find(p => String(p.id) === id)?.position ?? 'Desconocida';

      const current =
        byPlayer.get(id) ?? { id, name, position, goals: 0, assists: 0, minutes: 0, shotsOnTarget: 0, shotsTotal: 0, passesCompleted: 0, passesTotal: 0, duelsWon: 0, duelsTotal: 0, interceptions: 0, saves: 0 };

      current.goals += s.goals ?? 0;
      current.assists += s.assists ?? 0;
      current.minutes += s.minutesPlayed ?? 0;
      current.shotsOnTarget += s.shotsOnTarget ?? 0;
      current.shotsTotal += s.shotsTotal ?? 0;
      current.passesCompleted += s.passesCompleted ?? 0;
      current.passesTotal += s.passesTotal ?? 0;
      current.duelsWon += s.duelsWon ?? 0;
      current.duelsTotal += s.duelsTotal ?? 0;
      current.interceptions += s.interceptions ?? 0;
      current.saves += s.saves ?? 0;
      byPlayer.set(id, current);
    }

    return [...byPlayer.values()]
      .sort((a, b) => b.goals - a.goals || b.assists - a.assists || b.minutes - a.minutes);
  }, [effectiveFilterMode, stats, players.data]);

  const groupedPlayers = useMemo(() => {
    if (!totalsByPlayer) return null;
    
    const groups: Record<string, typeof totalsByPlayer> = {
      Porteros: [],
      Defensas: [],
      Centrocampistas: [],
      Delanteros: [],
      Otros: []
    };

    totalsByPlayer.forEach(p => {
      const pos = p.position.toLowerCase();
      if (pos.includes('por')) groups.Porteros.push(p);
      else if (pos.includes('med') || pos.includes('centro') || pos.includes('piv') || pos.includes('int') || pos.includes('mco') || pos.includes('mp')) groups.Centrocampistas.push(p);
      else if (pos.includes('def') || pos.includes('lat') || pos.includes('central') || pos === 'df' || pos === 'ct' || pos === 'cb') groups.Defensas.push(p);
      else if (pos.includes('del') || pos.includes('ext') || pos.includes('pun') || pos.includes('dc')) groups.Delanteros.push(p);
      else groups.Otros.push(p);
    });

    return groups;
  }, [totalsByPlayer]);

  return (
    <section className="page">
      <Card title="Consultar rendimiento" subtitle="Elige un equipo o un jugador para ver sus datos">
        <div className="form stats-filters">
          {!isUser && (
            <div className="stats-filters__modes">
              <button
                type="button"
                className={`stats-filters__mode${filterMode === 'team' ? ' stats-filters__mode--active' : ''}`}
                onClick={() => handleModeChange('team')}
              >
                Por equipo
              </button>
              <button
                type="button"
                className={`stats-filters__mode${filterMode === 'player' ? ' stats-filters__mode--active' : ''}`}
                onClick={() => handleModeChange('player')}
              >
                Por jugador
              </button>
            </div>
          )}

          {effectiveFilterMode === 'team' ? (
            <Select
              label="Equipo"
              value={teamId}
              onChange={(e) => setTeamId(e.target.value)}
              disabled={isUser}
            >
              <option value="">Selecciona un equipo…</option>
              {teams.data?.map((t) => (
                <option key={t.id} value={String(t.id)}>
                  {t.name}
                </option>
              ))}
            </Select>
          ) : (
            isUser ? null : (
              <Select
                label="Jugador"
                value={playerId}
                onChange={(e) => setPlayerId(e.target.value)}
              >
                <option value="">Selecciona un jugador…</option>
                {players.data?.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.name} · #{p.dorsal}
                  </option>
                ))}
              </Select>
            )
          )}

          {isUser && (
            <div>
              <Input
                label="Jugador (escribe el nombre)"
                value={playerQuery}
                onChange={(e) => setPlayerQuery(e.target.value)}
                list="player-names"
                placeholder="Ej: Álvaro Mendoza"
              />
              <datalist id="player-names">
                {(players.data ?? [])
                  .filter((p) => p.team_id === teamId)
                  .map((p) => (
                    <option key={p.id} value={p.name} />
                  ))}
              </datalist>
              {playerQuery.trim() !== '' && !matchedPlayerId && (
                <p className="muted stats-filters__hint">Selecciona un nombre de la lista.</p>
              )}
              {matchedPlayerId && (
                <Button type="button" variant="ghost" disabled={loading} onClick={() => setPlayerQuery('')}>
                  Ver por equipo
                </Button>
              )}
            </div>
          )}

          {canExportReports && hasSelection && (
            <Button
              type="button"
              variant="secondary"
              disabled={exporting || loading}
              onClick={() => void handleExportPdf()}
            >
              <FileDown size={16} />
              {exporting ? 'Generando PDF…' : 'Descargar informe PDF'}
            </Button>
          )}
        </div>
      </Card>

      <Card
        title={
          hasSelection
            ? effectiveFilterMode === 'team'
              ? `Estadísticas · ${selectionLabel}`
              : `Estadísticas · ${selectionLabel}`
            : 'Tabla de rendimiento'
        }
        subtitle={hasSelection ? undefined : 'Sin filtro aplicado'}
      >
        {!hasSelection && (
          <EmptyState
            icon={<BarChart3 size={32} />}
            title="Selecciona equipo o jugador"
            description="Las estadísticas solo se muestran cuando eliges un equipo o un jugador concreto."
          />
        )}

        {hasSelection && loading && (
          <div className="page__center">
            <Spinner />
          </div>
        )}

        {hasSelection && error && <Alert>{error}</Alert>}

        {hasSelection && !loading && !error && stats?.length === 0 && (
          <EmptyState
            icon={<BarChart3 size={32} />}
            title="Sin estadísticas"
            description={
              filterMode === 'team'
                ? 'Este equipo aún no tiene registros. Configura convocatorias y rendimiento en vivo.'
                : 'Este jugador aún no tiene registros de rendimiento.'
            }
          />
        )}

        {effectiveFilterMode === 'team' && hasSelection && !loading && !error && (totalsByPlayer?.length ?? 0) > 0 && groupedPlayers && (
          <div className="stats-groups">
            {Object.entries(groupedPlayers).map(([groupName, playersInGroup]) => {
              if (playersInGroup.length === 0) return null;
              
              const isGK = groupName === 'Porteros';
              const isDEF = groupName === 'Defensas';
              const isMID = groupName === 'Centrocampistas';
              const isFWD = groupName === 'Delanteros';

              return (
                <div key={groupName} style={{ marginBottom: '2rem' }}>
                  <h3 style={{ marginBottom: '1rem', color: 'var(--accent)', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem', fontSize: '1.2rem', fontWeight: 700 }}>{groupName}</h3>
                  <div className="table-wrap" aria-label={`Totales de ${groupName}`}>
                    <table className="table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr style={{ borderBottom: '2px solid var(--border)' }}>
                          <th style={{ textAlign: 'left', padding: '1rem' }}>Jugador</th>
                          <th style={{ textAlign: 'center', padding: '1rem' }}>Min</th>
                          {(isFWD || isMID || isDEF) && <th style={{ textAlign: 'center', padding: '1rem' }}>Goles</th>}
                          {(isFWD || isMID) && <th style={{ textAlign: 'center', padding: '1rem' }}>Asistencias</th>}
                          {(isFWD || isMID) && <th style={{ textAlign: 'center', padding: '1rem' }}>Tiros (Pta)</th>}
                          {(isMID || isDEF || isGK) && <th style={{ textAlign: 'center', padding: '1rem' }}>Pases (%)</th>}
                          {(isMID || isDEF) && <th style={{ textAlign: 'center', padding: '1rem' }}>Duelos (%)</th>}
                          {(isDEF || isMID) && <th style={{ textAlign: 'center', padding: '1rem' }}>Robos</th>}
                          {isGK && <th style={{ textAlign: 'center', padding: '1rem' }}>Paradas</th>}
                          {!isGK && !isDEF && !isMID && !isFWD && (
                             <>
                                <th style={{ textAlign: 'center', padding: '1rem' }}>Goles</th>
                                <th style={{ textAlign: 'center', padding: '1rem' }}>Asistencias</th>
                             </>
                          )}
                        </tr>
                      </thead>
                      <tbody>
                        {playersInGroup.map((p) => (
                          <tr key={p.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background 0.2s' }}>
                            <td style={{ padding: '1rem' }}><strong>{p.name}</strong></td>
                            <td style={{ textAlign: 'center', padding: '1rem' }}>{p.minutes}</td>
                            {(isFWD || isMID || isDEF) && <td style={{ textAlign: 'center', padding: '1rem', fontWeight: p.goals > 0 ? 800 : 400 }} className={p.goals > 0 ? "text-accent" : ""}>{p.goals}</td>}
                            {(isFWD || isMID) && <td style={{ textAlign: 'center', padding: '1rem', fontWeight: p.assists > 0 ? 700 : 400 }}>{p.assists}</td>}
                            {(isFWD || isMID) && <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(p.shotsOnTarget, p.shotsTotal)}</td>}
                            {(isMID || isDEF || isGK) && <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(p.passesCompleted, p.passesTotal)}</td>}
                            {(isMID || isDEF) && <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(p.duelsWon, p.duelsTotal)}</td>}
                            {(isDEF || isMID) && <td style={{ textAlign: 'center', padding: '1rem' }}>{p.interceptions}</td>}
                            {isGK && <td style={{ textAlign: 'center', padding: '1rem', fontWeight: p.saves > 0 ? 700 : 400 }} className={p.saves > 0 ? "text-accent" : ""}>{p.saves}</td>}
                            {!isGK && !isDEF && !isMID && !isFWD && (
                               <>
                                 <td style={{ textAlign: 'center', padding: '1rem' }}>{p.goals}</td>
                                 <td style={{ textAlign: 'center', padding: '1rem' }}>{p.assists}</td>
                               </>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {effectiveFilterMode === 'player' && hasSelection && !loading && !error && (stats?.length ?? 0) > 0 && (
          <div className="player-dashboard">
            <div className="player-dashboard__radar">
              <h3 className="player-dashboard__title" style={{ marginBottom: '1rem', fontSize: '1.1rem', fontWeight: 600 }}>Perfil Táctico (Percentiles)</h3>
              <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart cx="50%" cy="50%" outerRadius="70%" data={mockRadarData}>
                    <PolarGrid stroke="rgba(255, 255, 255, 0.15)" />
                    <PolarAngleAxis dataKey="metric" tick={{ fill: 'var(--text-muted)', fontSize: 12 }} />
                    <PolarRadiusAxis angle={30} domain={[0, 100]} tick={false} axisLine={false} />
                    <Radar
                      name="Percentil"
                      dataKey="value"
                      stroke="var(--primary)"
                      fill="var(--primary)"
                      fillOpacity={0.4}
                    />
                    <RechartsTooltip 
                      contentStyle={{ backgroundColor: 'var(--surface-sunken)', border: '1px solid var(--border)', borderRadius: '8px', color: 'var(--text)' }} 
                      itemStyle={{ color: 'var(--primary)', fontWeight: 'bold' }}
                    />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            </div>
            
            <div className="player-dashboard__table" style={{ marginTop: '2rem' }}>
              <h3 className="player-dashboard__title" style={{ marginBottom: '1rem', fontSize: '1.1rem', fontWeight: 600 }}>Desglose por Partido</h3>
              <div className="table-wrap" aria-label="Registros del jugador">
                <table className="table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid var(--border)' }}>
                      <th style={{ textAlign: 'left', padding: '1rem' }}>Partido</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Min</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Goles</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Asistencias</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Tiros (Pta)</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Pases (%)</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Duelos (%)</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Robos</th>
                      <th style={{ textAlign: 'center', padding: '1rem' }}>Paradas</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats?.map((s) => (
                      <tr key={s.id ?? `${s.match}-${s.player?.id ?? 'p'}`} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '1rem' }}><strong>{s.match}</strong></td>
                        <td style={{ textAlign: 'center', padding: '1rem' }}>{s.minutesPlayed ?? 0}</td>
                        <td style={{ textAlign: 'center', padding: '1rem', fontWeight: (s.goals ?? 0) > 0 ? 800 : 400 }} className={(s.goals ?? 0) > 0 ? "text-accent" : ""}>{s.goals ?? 0}</td>
                        <td style={{ textAlign: 'center', padding: '1rem', fontWeight: (s.assists ?? 0) > 0 ? 700 : 400 }}>{s.assists ?? 0}</td>
                        <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(s.shotsOnTarget, s.shotsTotal)}</td>
                        <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(s.passesCompleted, s.passesTotal)}</td>
                        <td style={{ textAlign: 'center', padding: '0.5rem' }}>{formatFraction(s.duelsWon, s.duelsTotal)}</td>
                        <td style={{ textAlign: 'center', padding: '1rem' }}>{s.interceptions ?? '-'}</td>
                        <td style={{ textAlign: 'center', padding: '1rem' }}>{s.saves ?? '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {effectiveFilterMode === 'team' && hasSelection && filteredPlayers && filteredPlayers.length > 0 && (
          <p className="muted stats-filters__hint">
            Plantilla del equipo: {filteredPlayers.length} jugador
            {filteredPlayers.length === 1 ? '' : 'es'}
          </p>
        )}
      </Card>
    </section>
  );
}
