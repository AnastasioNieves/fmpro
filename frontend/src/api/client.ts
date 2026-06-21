const API_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
import { auth } from '../firebase';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

function parseErrorMessage(text: string, fallback: string): string {
  if (!text) return fallback;
  try {
    const json = JSON.parse(text) as { message?: string };
    return json.message || text;
  } catch {
    return text;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (!API_URL) {
    throw new ApiError(
      'Configura VITE_API_URL con la URL de tu backend (http://localhost:8080).',
      0,
    );
  }

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  const currentUser = auth.currentUser;
  if (currentUser) {
    try {
      const token = await currentUser.getIdToken();
      (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    } catch (e) {
      console.warn("Could not get firebase token", e);
    }
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new ApiError(
      parseErrorMessage(text, response.statusText),
      response.status,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    return response.json() as Promise<T>;
  }

  return undefined as T;
}

async function requestBlob(path: string): Promise<Blob> {
  if (!API_URL) {
    throw new ApiError(
      'Configura VITE_API_URL con la URL de tu backend (http://localhost:8080).',
      0,
    );
  }

  const headers: Record<string, string> = {};
  const currentUser = auth.currentUser;
  if (currentUser) {
    try {
      const token = await currentUser.getIdToken();
      headers['Authorization'] = `Bearer ${token}`;
    } catch (e) {
      console.warn("Could not get firebase token", e);
    }
  }

  const response = await fetch(`${API_URL}${path}`, {
    headers,
    credentials: 'include' // 👈 ESTE ES EL FIX
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new ApiError(
      parseErrorMessage(text, response.statusText),
      response.status,
    );
  }

  return response.blob();
}

async function uploadFile<T>(path: string, file: File, fieldName = 'file'): Promise<T> {
  if (!API_URL) {
    throw new ApiError(
      'Configura VITE_API_URL con la URL de tu backend (http://localhost:8080).',
      0,
    );
  }

  const form = new FormData();
  form.append(fieldName, file);

  const headers: Record<string, string> = {};
  const currentUser = auth.currentUser;
  if (currentUser) {
    try {
      const token = await currentUser.getIdToken();
      headers['Authorization'] = `Bearer ${token}`;
    } catch (e) {
      console.warn("Could not get firebase token", e);
    }
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    body: form,
    headers,
    credentials: 'include',
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new ApiError(
      parseErrorMessage(text, response.statusText),
      response.status,
    );
  }

  return response.json() as Promise<T>;
}

export function assetUrl(path?: string | null): string | null {
  if (!path) return null;
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${API_URL}${path.startsWith('/') ? path : `/${path}`}`;
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
  download: (path: string) => requestBlob(path),
  upload: uploadFile,
};

export const endpoints = {
  teams: '/api/teams',
  teamsPublic: '/api/teams/public',
  matchScore: (id: string | number) => `/api/matches/${id}/score`,
  matchClose: (id: string | number) => `/api/matches/${id}/close`,
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    logout: '/api/auth/logout',
    me: '/api/auth/me',
  },
  players: '/api/players',
  playerPhoto: (id: string | number) => `/api/players/${id}/photo`,
  matches: '/api/matches',
  match: (id: string | number) => `/api/matches/${id}`,
  matchSquad: (id: string | number) => `/api/matches/${id}/squad`,
  matchLineup: (id: string | number) => `/api/matches/${id}/lineup`,
  matchLiveStats: (id: string | number) => `/api/matches/${id}/live-stats`,
  statistics: '/api/statistics',
  statisticsSummary: '/api/statistics/summary',
  statisticsByTeam: (teamId: string | number) => `/api/statistics/team/${teamId}`,
  statisticsByPlayer: (playerId: string | number) => `/api/statistics/player/${playerId}`,
  reportTeam: (teamId: string | number) => `/api/reports/teams/${teamId}`,
  reportPlayer: (playerId: string | number) => `/api/reports/players/${playerId}`,
  roles: '/api/roles',
  rolesRegisterable: '/api/roles/registerable',
};
