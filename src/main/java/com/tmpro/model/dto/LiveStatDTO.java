package com.tmpro.model.dto;

import com.tmpro.model.Statistic;

public class LiveStatDTO {

    private Long id;
    private Long playerId;
    private String playerName;
    private String dorsal;
    private String position;
    private int goals;
    private int assists;
    private int minutesPlayed;
    private boolean starter;

    public LiveStatDTO() {
    }

    public LiveStatDTO(Statistic stat, boolean starter) {
        this.id = stat.getId();
        this.playerId = stat.getPlayer().getId();
        this.playerName = stat.getPlayer().getName();
        this.dorsal = stat.getPlayer().getDorsal();
        this.position = stat.getPlayer().getPosition();
        this.goals = stat.getGoals();
        this.assists = stat.getAssists();
        this.minutesPlayed = stat.getMinutesPlayed();
        this.starter = starter;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getDorsal() {
        return dorsal;
    }

    public void setDorsal(String dorsal) {
        this.dorsal = dorsal;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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

    public boolean isStarter() {
        return starter;
    }

    public void setStarter(boolean starter) {
        this.starter = starter;
    }
}
