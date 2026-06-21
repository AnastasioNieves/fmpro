package com.tmpro.model.dto;

import com.tmpro.model.Statistic;
import com.tmpro.model.Player;

public class LiveStatDTO {

    private String id;
    private String playerId;
    private String playerName;
    private String dorsal;
    private String position;
    private int goals;
    private int assists;
    private int minutesPlayed;
    private boolean starter;

    public LiveStatDTO() {
    }

    public LiveStatDTO(Statistic stat, Player player, boolean starter) {
        this.id = stat.getId();
        this.playerId = player.getId();
        this.playerName = player.getName();
        this.dorsal = player.getDorsal();
        this.position = player.getPosition();
        this.goals = stat.getGoals();
        this.assists = stat.getAssists();
        this.minutesPlayed = stat.getMinutesPlayed();
        this.starter = starter;
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
