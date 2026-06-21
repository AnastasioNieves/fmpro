import { FormEvent, useRef, useState } from 'react';
import { Camera, Trash2, UserRound } from 'lucide-react';
import { api, ApiError, assetUrl, endpoints } from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  Alert,
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  Select,
  Spinner,
} from '../components/ui';
import { useFetch } from '../hooks/useFetch';
import type { Player, Team } from '../types';

const emptyPlayer: Player = {
  name: '',
  position: '',
  dorsal: '',
  team_id: '',
};

const POSITIONS = ['Portero', 'Defensa', 'Centrocampista', 'Delantero'];

export function Players() {
  const { canManageTeams } = useAuth();
  const players = useFetch(() => api.get<Player[]>(endpoints.players), []);
  const teams = useFetch(() => api.get<Team[]>(endpoints.teams), []);
  const [form, setForm] = useState<Player>(emptyPlayer);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [uploadingId, setUploadingId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [photoTargetId, setPhotoTargetId] = useState<string | null>(null);

  const teamName = (id: string) =>
    teams.data?.find((t) => String(t.id) === id)?.name ?? '—';

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setFormError(null);
    try {
      if (editingId) {
        await api.put(`${endpoints.players}/${editingId}`, form);
      } else {
        await api.post(endpoints.players, form);
      }
      setForm(emptyPlayer);
      setEditingId(null);
      await players.reload();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo guardar');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('¿Eliminar este jugador?')) return;
    await api.delete(`${endpoints.players}/${id}`);
    await players.reload();
  }

  function startEdit(player: Player) {
    setEditingId(player.id ? String(player.id) : null);
    setForm({
      name: player.name,
      position: player.position,
      dorsal: player.dorsal,
      team_id: player.team_id,
    });
  }

  function openPhotoPicker(playerId: string) {
    setPhotoTargetId(playerId);
    fileInputRef.current?.click();
  }

  async function handlePhotoSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    const targetId = photoTargetId;
    e.target.value = '';
    setPhotoTargetId(null);
    if (!file || !targetId) return;

    setUploadingId(targetId);
    try {
      await api.upload<Player>(endpoints.playerPhoto(targetId), file);
      await players.reload();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo subir la foto');
    } finally {
      setUploadingId(null);
    }
  }

  return (
    <div className="page page--split">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="sr-only"
        aria-hidden
        onChange={(e) => void handlePhotoSelected(e)}
      />

      {canManageTeams && (
      <Card title={editingId ? 'Editar jugador' : 'Nuevo jugador'}>
        <form className="form" onSubmit={handleSubmit}>
          <Input
            label="Nombre"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
          <Select
            label="Posición"
            value={form.position}
            onChange={(e) => setForm({ ...form, position: e.target.value })}
            required
          >
            <option value="">Selecciona…</option>
            {POSITIONS.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </Select>
          <Input
            label="Dorsal"
            value={form.dorsal}
            onChange={(e) => setForm({ ...form, dorsal: e.target.value })}
            required
          />
          <Select
            label="Equipo"
            value={form.team_id}
            onChange={(e) => setForm({ ...form, team_id: e.target.value })}
            required
          >
            <option value="">Selecciona…</option>
            {teams.data?.map((t) => (
              <option key={t.id} value={String(t.id)}>
                {t.name}
              </option>
            ))}
          </Select>
          {formError && <Alert>{formError}</Alert>}
          <div className="form__actions">
            <Button type="submit" disabled={submitting}>
              {editingId ? 'Actualizar' : 'Añadir jugador'}
            </Button>
            {editingId && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setEditingId(null);
                  setForm(emptyPlayer);
                }}
              >
                Cancelar
              </Button>
            )}
          </div>
        </form>
      </Card>
      )}

      <Card title="Plantilla" subtitle={`${players.data?.length ?? 0} jugadores`}>
        {players.loading && (
          <div className="page__center">
            <Spinner />
          </div>
        )}
        {players.error && <Alert>{players.error}</Alert>}
        {!players.loading && players.data?.length === 0 && (
          <EmptyState
            icon={<UserRound size={32} />}
            title="Sin jugadores"
            description="Registra jugadores y asígnalos a un equipo."
          />
        )}
        <ul className="player-grid">
          {players.data?.map((player) => {
            const photoSrc = assetUrl(player.photoUrl);
            return (
              <li key={player.id} className="player-card">
                <div className="player-card__media">
                  {photoSrc ? (
                    <img src={photoSrc} alt={player.name} className="player-card__photo" />
                  ) : (
                    <div className="player-card__placeholder" aria-hidden>
                      <UserRound size={40} />
                    </div>
                  )}
                  <span className="player-card__dorsal">#{player.dorsal}</span>
                  {canManageTeams && (
                  <button
                    type="button"
                    className="player-card__photo-btn"
                    title="Cambiar foto"
                    disabled={uploadingId === player.id}
                    onClick={() => openPhotoPicker(String(player.id))}
                  >
                    <Camera size={16} />
                    {uploadingId === player.id ? 'Subiendo…' : 'Foto'}
                  </button>
                  )}
                </div>
                <div className="player-card__body">
                  <h3>{player.name}</h3>
                  <p>{player.position}</p>
                  <p className="muted">{teamName(player.team_id)}</p>
                  <Badge>{player.position}</Badge>
                </div>
                {canManageTeams && (
                <div className="player-card__actions">
                  <Button variant="ghost" onClick={() => startEdit(player)}>
                    Editar
                  </Button>
                  <Button variant="danger" onClick={() => handleDelete(String(player.id))}>
                    <Trash2 size={16} />
                  </Button>
                </div>
                )}
              </li>
            );
          })}
        </ul>
      </Card>
    </div>
  );
}
