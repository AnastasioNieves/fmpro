package com.tmpro.model;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class Match {

    private String id;
    private Date date;
    private String location;
    private String teamId;
    private String opponentName = "Rival";
    private MatchStatus status = MatchStatus.SCHEDULED;
    private Boolean home = true;
    private Integer teamScore = 0;
    private Integer opponentScore = 0;

    // En Firestore usaremos arrays de strings para los IDs de los jugadores
    private List<String> squadPlayerIds = new ArrayList<>();
    private List<String> lineupPlayerIds = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public MatchStatus getStatus() {
        return status != null ? status : MatchStatus.SCHEDULED;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public boolean isHome() {
        return home == null || home;
    }

    public void setHome(Boolean home) {
        this.home = home == null ? true : home;
    }

    public int getTeamScore() {
        return teamScore != null ? teamScore : 0;
    }

    public void setTeamScore(int teamScore) {
        this.teamScore = teamScore;
    }

    public int getOpponentScore() {
        return opponentScore != null ? opponentScore : 0;
    }

    public void setOpponentScore(int opponentScore) {
        this.opponentScore = opponentScore;
    }

    public List<String> getSquadPlayerIds() {
        return squadPlayerIds;
    }

    public void setSquadPlayerIds(List<String> squadPlayerIds) {
        this.squadPlayerIds = squadPlayerIds != null ? squadPlayerIds : new ArrayList<>();
    }

    public List<String> getLineupPlayerIds() {
        return lineupPlayerIds;
    }

    public void setLineupPlayerIds(List<String> lineupPlayerIds) {
        this.lineupPlayerIds = lineupPlayerIds != null ? lineupPlayerIds : new ArrayList<>();
    }
}
