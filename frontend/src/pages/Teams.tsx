import { FormEvent, useState } from 'react';
import { Shield, Trash2 } from 'lucide-react';
import { api, ApiError, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Alert, Button, Card, EmptyState, Input, Spinner } from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Team } from '../types';

const emptyTeam: Team = { name: '', coach: '' };

export function Teams() {
  const { canManageTeams } = useAuth();
  const { data, loading, error, reload } = useFetch(
    () => api.get<Team[]>(endpoints.teams),
    [],
  );
  const [form, setForm] = useState<Team>(emptyTeam);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editingId) {
        await api.put<Team>(`${endpoints.teams}/${editingId}`, form);
      } else {
        await api.post<Team>(endpoints.teams, form);
      }
      setForm(emptyTeam);
      setEditingId(null);
      await reload();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('¿Eliminar este equipo?')) return;
    await api.delete(`${endpoints.teams}/${id}`);
    await reload();
  }

  function startEdit(team: Team) {
    setEditingId(team.id ?? null);
    setForm({ name: team.name, coach: team.coach });
  }

  return (
    <div className="page page--split">
      {canManageTeams && (
      <Card
        title={editingId ? 'Editar equipo' : 'Nuevo equipo'}
        subtitle="Solo verás y gestionarás los equipos que crees"
      >
        <form className="form" onSubmit={handleSubmit}>
          <Input
            label="Nombre del equipo"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <Input
            label="Entrenador"
            value={form.coach}
            onChange={(e) => setForm({ ...form, coach: e.target.value })}
            required
          />
          {formError && <Alert>{formError}</Alert>}
          <div className="form__actions">
            <Button type="submit" disabled={submitting}>
              {editingId ? 'Actualizar' : 'Crear equipo'}
            </Button>
            {editingId && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setEditingId(null);
                  setForm(emptyTeam);
                }}
              >
                Cancelar
              </Button>
            )}
          </div>
        </form>
      </Card>
      )}

      <Card title="Plantilla de clubes" subtitle={`${data?.length ?? 0} equipos`}>
        {loading && (
          <div className="page__center">
            <Spinner />
          </div>
        )}
        {error && <Alert>{error}</Alert>}
        {!loading && !error && data?.length === 0 && (
          <EmptyState
            icon={<Shield size={32} />}
            title="Sin equipos"
            description="Crea tu primer equipo con el formulario."
          />
        )}
        <ul className="entity-grid">
          {data?.map((team) => (
            <li key={team.id} className="entity-card">
              <div className="entity-card__icon">
                <Shield size={22} />
              </div>
              <div className="entity-card__body">
                <h3>{team.name}</h3>
                <p>Entrenador: {team.coach}</p>
              </div>
              {canManageTeams && (
                <div className="entity-card__actions">
                  <Button variant="ghost" onClick={() => startEdit(team)}>
                    Editar
                  </Button>
                  <Button variant="danger" onClick={() => handleDelete(team.id!)}>
                    <Trash2 size={16} />
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
