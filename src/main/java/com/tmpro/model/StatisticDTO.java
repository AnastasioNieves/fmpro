package com.tmpro.model;

public class StatisticDTO {
    private String id;
    private PlayerDTO player;
    private String match;
    private int goals;
    private int assists;
    private int minutesPlayed;
    private int shotsTotal;
    private int shotsOnTarget;
    private int passesTotal;
    private int passesCompleted;
    private int duelsTotal;
    private int duelsWon;
    private int interceptions;
    private int saves;
    private int goalsConceded;

    public StatisticDTO(Statistic statistic, Player p) {
        this.id = statistic.getId();
        if (p != null) {
            this.player = new PlayerDTO(p);
        }
        this.match = statistic.getMatch();
        this.goals = statistic.getGoals();
        this.assists = statistic.getAssists();
        this.minutesPlayed = statistic.getMinutesPlayed();
        this.shotsTotal = statistic.getShotsTotal();
        this.shotsOnTarget = statistic.getShotsOnTarget();
        this.passesTotal = statistic.getPassesTotal();
        this.passesCompleted = statistic.getPassesCompleted();
        this.duelsTotal = statistic.getDuelsTotal();
        this.duelsWon = statistic.getDuelsWon();
        this.interceptions = statistic.getInterceptions();
        this.saves = statistic.getSaves();
        this.goalsConceded = statistic.getGoalsConceded();
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PlayerDTO getPlayer() {
        return player;
    }

    public void setPlayer(PlayerDTO player) {
        this.player = player;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public void setMinutesPlayed(int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
    }

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
