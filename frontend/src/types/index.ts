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
}

export type MatchStatus = 'SCHEDULED' | 'FINISHED';

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
