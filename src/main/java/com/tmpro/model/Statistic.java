package com.tmpro.model;

public class Statistic {

    private String id;
    private String playerId;
    private String matchId; // Puede ser null si es global
    private String match = ""; // Nombre del partido

    private int goals = 0;
    private int assists = 0;
    private int minutesPlayed = 0;
    private int shotsTotal = 0;
    private int shotsOnTarget = 0;
    private int passesTotal = 0;
    private int passesCompleted = 0;
    private int duelsTotal = 0;
    private int duelsWon = 0;
    private int interceptions = 0;
    private int saves = 0;
    private int goalsConceded = 0;

    public Statistic() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public int getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(int minutesPlayed) { this.minutesPlayed = minutesPlayed; }

    public int getShotsTotal() { return shotsTotal; }
    public void setShotsTotal(int shotsTotal) { this.shotsTotal = shotsTotal; }

    public int getShotsOnTarget() { return shotsOnTarget; }
    public void setShotsOnTarget(int shotsOnTarget) { this.shotsOnTarget = shotsOnTarget; }

    public int getPassesTotal() { return passesTotal; }
    public void setPassesTotal(int passesTotal) { this.passesTotal = passesTotal; }

    public int getPassesCompleted() { return passesCompleted; }
    public void setPassesCompleted(int passesCompleted) { this.passesCompleted = passesCompleted; }

    public int getDuelsTotal() { return duelsTotal; }
    public void setDuelsTotal(int duelsTotal) { this.duelsTotal = duelsTotal; }

    public int getDuelsWon() { return duelsWon; }
    public void setDuelsWon(int duelsWon) { this.duelsWon = duelsWon; }

    public int getInterceptions() { return interceptions; }
    public void setInterceptions(int interceptions) { this.interceptions = interceptions; }

    public int getSaves() { return saves; }
    public void setSaves(int saves) { this.saves = saves; }

    public int getGoalsConceded() { return goalsConceded; }
    public void setGoalsConceded(int goalsConceded) { this.goalsConceded = goalsConceded; }
}
