export interface Team {
  id?: string | number;
  name: string;
  coach: string;
}

export interface Player {
  id?: string | number;
  name: string;
  position: string;
  dorsal: string;
  team_id: string;
  photoUrl?: string | null;
}

export interface StatisticsSummary {
  totalRecords: number;
  totalGoals: number;
  totalAssists: number;
}

export interface PlayerSummary {
  id: string | number;
  name: string;
  position: string;
  dorsal: string;
}

export interface LiveStat {
  id?: string | number;
  playerId: string;
  playerName: string;
  dorsal: string;
  position: string;
  goals: number;
  assists: number;
  minutesPlayed: number;
  starter: boolean;
  shotsTotal?: number;
  shotsOnTarget?: number;
  passesTotal?: number;
  passesCompleted?: number;
  duelsTotal?: number;
  duelsWon?: number;
  interceptions?: number;
  saves?: number;
  goalsConceded?: number;
}

export type MatchStatus = 'SCHEDULED' | 'FINISHED';
export type FormationType = '4-3-3' | '4-4-2' | '3-5-2' | '4-2-3-1' | 'Personalizada';

export interface MatchDetail {
  id: string | number;
  date: string;
  location: string;
  teamId?: string | number;
  opponentName?: string;
  status?: MatchStatus;
  home?: boolean;
  teamScore: number;
  opponentScore: number;
  squad: PlayerSummary[];
  lineup: PlayerSummary[];
  liveStats: LiveStat[];
  formation?: FormationType;
  lineupPositions?: Record<string, string>;
}

export interface Match {
  id?: string | number;
  date: string;
  location: string;
  teamId?: string | number;
  opponentName?: string;
  status?: MatchStatus;
  home?: boolean;
  teamScore?: number;
  opponentScore?: number;
  squadCount?: number;
  lineupCount?: number;
  lineup?: PlayerSummary[];
  squad?: PlayerSummary[];
}

export interface MatchScoreUpdate {
  teamScore?: number;
  opponentScore?: number;
  opponentName?: string;
}

export interface Statistic {
  id?: string | number;
  player?: Player;
  match: string;
  goals: number;
  assists: number;
  minutesPlayed: number;
  
  // Métricas avanzadas (opcionales hasta que el backend las soporte nativamente)
  shotsTotal?: number;
  shotsOnTarget?: number;
  passesTotal?: number;
  passesCompleted?: number;
  duelsTotal?: number;
  duelsWon?: number;
  interceptions?: number;
  saves?: number;
  goalsConceded?: number;
}

export interface Role {
  id: string | number;
  name: string;
}

export interface AuthUser {
  id: string | number;
  username: string;
  roleId: string | number;
  roleName: string;
  teamId?: string | number | null;
}

export interface UserRequest {
  username: string;
  password: string;
  roleId: string | number;
  teamId?: string | number;
}

export interface UserResponse {
  id: string | number;
  username: string;
  roleId: string | number;
  roleName: string;
}
