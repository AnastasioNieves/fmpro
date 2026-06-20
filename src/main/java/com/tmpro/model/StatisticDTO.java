package com.tmpro.model;

public class StatisticDTO {
    private Long id;
    private PlayerDTO player;
    private String match;
    private int goals;
    private int assists;
    private int minutesPlayed;

    // Constructor que acepta un objeto Statistic
    public StatisticDTO(Statistic statistic) {
        this.id = statistic.getId();
        if (statistic.getPlayer() != null) {
            this.player = new PlayerDTO(statistic.getPlayer());
        }
        this.match = statistic.getMatch();
        this.goals = statistic.getGoals();
        this.assists = statistic.getAssists();
        this.minutesPlayed = statistic.getMinutesPlayed();
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
}
