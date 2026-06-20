import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BarChart3, Calendar, Eye, MapPin, Settings2, Trash2 } from 'lucide-react';
import { api, ApiError, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Alert, Badge, Button, Card, EmptyState, Input, Select, Spinner } from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Match, Team } from '../types';

function toLocalInputValue(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function Matches() {
  const navigate = useNavigate();
  const { canManageMatches, isUser } = useAuth();
  const teams = useFetch(() => api.get<Team[]>(endpoints.teams), []);
  const { data, loading, error, reload } = useFetch(
    () => api.get<Match[]>(endpoints.matches),
    [],
  );
  const [teamId, setTeamId] = useState('');
  const [location, setLocation] = useState('');
  const [opponentName, setOpponentName] = useState('Rival');
  const [home, setHome] = useState(true);
  const [date, setDate] = useState(toLocalInputValue(new Date()));
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const teamNameById = new Map<number, string>(
    (teams.data ?? []).flatMap((t) => (t.id != null ? [[t.id, t.name] as const] : [])),
  );

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!teamId) {
      setFormError('Selecciona un equipo');
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      const created = await api.post<Match>(endpoints.matches, {
        teamId: Number(teamId),
        location,
        opponentName: opponentName.trim() || 'Rival',
        home,
        date: new Date(date).toISOString(),
        teamScore: 0,
        opponentScore: 0,
      });
      setLocation('');
      setOpponentName('Rival');
      setHome(true);
      setDate(toLocalInputValue(new Date()));
      await reload();
      if (created.id && canManageMatches) {
        navigate(`/partidos/${created.id}`);
      }
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo crear');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('¿Eliminar este partido?')) return;
    await api.delete(`${endpoints.matches}/${id}`);
    await reload();
  }

  return (
    <section className={canManageMatches ? 'page page--split matches-page' : 'page matches-page'}>
      {canManageMatches && (
        <aside className="matches-page__composer">
          <Card title="Nuevo partido" subtitle="Crea el encuentro y configura convocatoria">
            <form className="form" onSubmit={handleSubmit}>
              <Select
                label="Equipo"
                value={teamId}
                onChange={(e) => setTeamId(e.target.value)}
                required
              >
                <option value="">Selecciona…</option>
                {teams.data?.map((t) => (
                  <option key={t.id} value={String(t.id)}>
                    {t.name}
                  </option>
                ))}
              </Select>
              <Input
                label="Campo / estadio"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                required
              />
              <Input
                label="Rival"
                value={opponentName}
                onChange={(e) => setOpponentName(e.target.value)}
                required
              />
              <Select
                label="Condición"
                value={home ? 'home' : 'away'}
                onChange={(e) => setHome(e.target.value === 'home')}
                required
              >
                <option value="home">Local</option>
                <option value="away">Visitante</option>
              </Select>
              <Input
                label="Fecha y hora"
                type="datetime-local"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
              {formError && <Alert>{formError}</Alert>}
              <Button type="submit" disabled={submitting}>
                Crear y gestionar
              </Button>
            </form>
          </Card>
        </aside>
      )}

      <div className="matches-page__calendar">
        <Card
          title="Calendario"
          subtitle={isUser ? 'Marcador y estadísticas en vivo' : `${data?.length ?? 0} partidos`}
        >
          {loading && (
            <div className="page__center">
              <Spinner />
            </div>
          )}
          {error && <Alert>{error}</Alert>}
          {!loading && !error && data?.length === 0 && (
            <EmptyState
              icon={<Calendar size={32} />}
              title="Sin partidos"
              description={
                isUser
                  ? 'Tu equipo aún no tiene partidos programados.'
                  : 'Programa tu primer encuentro.'
              }
            />
          )}
          <ul className="entity-grid entity-grid--matches">
            {data?.map((match) => {
              const isHome = match.home ?? true;
              const finished = match.status === 'FINISHED';
              const teamName = teamNameById.get(match.teamId ?? -1) ?? 'Equipo';
              const opponent = match.opponentName ?? 'Rival';
              const homeTeam = isHome ? teamName : opponent;
              const awayTeam = isHome ? opponent : teamName;
              const homeScore = isHome ? (match.teamScore ?? 0) : (match.opponentScore ?? 0);
              const awayScore = isHome ? (match.opponentScore ?? 0) : (match.teamScore ?? 0);

              return (
                <li key={match.id} className="entity-card entity-card--wide match-card">
                  <div className="match-card__header">
                    <div className="match-card__location">
                      <MapPin size={16} />
                      <span>{match.location}</span>
                    </div>
                    <div className="match-card__badges">
                      <Badge>{isHome ? 'Local' : 'Visitante'}</Badge>
                      {finished && <Badge variant="success">Finalizado</Badge>}
                    </div>
                  </div>

                  <div className="match-card__meta">
                    <div className="match-card__scoreline" aria-label="Marcador">
                      <div className="match-card__team match-card__team--home">
                        <span className="match-card__team-name">{homeTeam}</span>
                      </div>
                      <div className="match-card__score">
                        <span className="match-card__score-num">{homeScore}</span>
                        <span className="match-card__score-sep">-</span>
                        <span className="match-card__score-num">{awayScore}</span>
                      </div>
                      <div className="match-card__team match-card__team--away">
                        <span className="match-card__team-name">{awayTeam}</span>
                      </div>
                    </div>

                    <div className="match-card__details">
                      <p className="match-card__date">
                        {new Date(match.date).toLocaleString('es-ES', {
                          dateStyle: 'medium',
                          timeStyle: 'short',
                        })}
                      </p>
                      {!isUser && (
                        <p className="muted match-card__counts">
                          {match.squadCount ?? 0} convocados · {match.lineupCount ?? 0} titulares
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="match-card__footer">
                    <div className="match-card__actions">
                      <Link to={`/partidos/${match.id}`} className="btn btn--primary">
                        {finished ? (
                          <>
                            <BarChart3 size={16} /> Ver estadísticas
                          </>
                        ) : isUser ? (
                          <>
                            <Eye size={16} /> Ver en vivo
                          </>
                        ) : (
                          <>
                            <Settings2 size={16} /> Gestionar
                          </>
                        )}
                      </Link>
                      {canManageMatches && (
                        <Button variant="danger" onClick={() => handleDelete(match.id!)}>
                          <Trash2 size={16} />
                        </Button>
                      )}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        </Card>
      </div>
    </section>
  );
}
