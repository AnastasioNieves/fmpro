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

type FilterMode = 'team' | 'player';

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
      { id: string; name: string; goals: number; assists: number; minutes: number }
    >();

    for (const s of stats) {
      const id =
        s.player?.id != null
          ? String(s.player.id)
          : s.player?.name
            ? `name:${s.player.name}`
            : `unknown:${s.id ?? Math.random()}`;
      const name = s.player?.name ?? '—';

      const current =
        byPlayer.get(id) ?? { id, name, goals: 0, assists: 0, minutes: 0 };

      current.goals += s.goals ?? 0;
      current.assists += s.assists ?? 0;
      current.minutes += s.minutesPlayed ?? 0;
      byPlayer.set(id, current);
    }

    return [...byPlayer.values()]
      .map((p) => ({ id: p.id, name: p.name, goals: p.goals, assists: p.assists, minutes: p.minutes }))
      .sort((a, b) => b.goals - a.goals || b.assists - a.assists || b.minutes - a.minutes);
  }, [effectiveFilterMode, stats]);

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

        {effectiveFilterMode === 'team' && hasSelection && !loading && !error && (totalsByPlayer?.length ?? 0) > 0 && (
          <div className="table-wrap" aria-label="Totales por jugador">
            <table className="table">
              <thead>
                <tr>
                  <th>Jugador</th>
                  <th>G</th>
                  <th>A</th>
                  <th>Min</th>
                </tr>
              </thead>
              <tbody>
                {totalsByPlayer?.map((p) => (
                  <tr key={p.id}>
                    <td>{p.name}</td>
                    <td>{p.goals}</td>
                    <td>{p.assists}</td>
                    <td>{p.minutes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {effectiveFilterMode === 'player' && hasSelection && !loading && !error && (stats?.length ?? 0) > 0 && (
          <div className="table-wrap" aria-label="Registros del jugador">
            <table className="table">
              <thead>
                <tr>
                  <th>Partido</th>
                  <th>G</th>
                  <th>A</th>
                  <th>Min</th>
                </tr>
              </thead>
              <tbody>
                {stats?.map((s) => (
                  <tr key={s.id ?? `${s.match}-${s.player?.id ?? 'p'}`}>
                    <td>{s.match}</td>
                    <td>{s.goals ?? 0}</td>
                    <td>{s.assists ?? 0}</td>
                    <td>{s.minutesPlayed ?? 0}</td>
                  </tr>
                ))}
              </tbody>
            </table>
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
