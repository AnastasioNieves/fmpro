export interface Team {
  id?: number;
  name: string;
  coach: string;
}

export interface Player {
  id?: number;
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
  id: number;
  name: string;
  position: string;
  dorsal: string;
}

export interface LiveStat {
  id?: number;
  playerId: number;
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
  id: number;
  date: string;
  location: string;
  teamId?: number;
  opponentName?: string;
  status?: MatchStatus;
  home?: boolean;
  teamScore: number;
  opponentScore: number;
  squad: PlayerSummary[];
  lineup: PlayerSummary[];
  liveStats: LiveStat[];
  formation?: FormationType;
  lineupPositions?: Record<number, string>;
}

export interface Match {
  id?: number;
  date: string;
  location: string;
  teamId?: number;
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
  id?: number;
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
  id: number;
  name: string;
}

export interface AuthUser {
  id: number;
  username: string;
  roleId: number;
  roleName: string;
  teamId?: number | null;
}

export interface UserRequest {
  username: string;
  password: string;
  roleId: number;
  teamId?: number;
}

export interface UserResponse {
  id: number;
  username: string;
  roleId: number;
  roleName: string;
}
