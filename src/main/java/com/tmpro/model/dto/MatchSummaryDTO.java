package com.tmpro.model.dto;

import com.tmpro.model.Match;
import com.tmpro.model.MatchStatus;

import java.util.Date;

public class MatchSummaryDTO {

    private String id;
    private Date date;
    private String location;
    private String teamId;
    private String opponentName;
    private MatchStatus status;
    private boolean home;
    private int teamScore;
    private int opponentScore;
    private int squadCount;
    private int lineupCount;

    public MatchSummaryDTO() {
    }

    public static MatchSummaryDTO from(Match match, int squadCount, int lineupCount) {
        MatchSummaryDTO dto = new MatchSummaryDTO();
        dto.id = match.getId();
        dto.date = match.getDate();
        dto.location = match.getLocation();
        dto.teamId = match.getTeamId();
        dto.opponentName = match.getOpponentName();
        dto.status = match.getStatus();
        dto.home = match.isHome();
        dto.teamScore = match.getTeamScore();
        dto.opponentScore = match.getOpponentScore();
        dto.squadCount = squadCount;
        dto.lineupCount = lineupCount;
        return dto;
    }

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
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public boolean isHome() {
        return home;
    }

    public void setHome(boolean home) {
        this.home = home;
    }

    public int getTeamScore() {
        return teamScore;
    }

    public void setTeamScore(int teamScore) {
        this.teamScore = teamScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public void setOpponentScore(int opponentScore) {
        this.opponentScore = opponentScore;
    }

    public int getSquadCount() {
        return squadCount;
    }

    public void setSquadCount(int squadCount) {
        this.squadCount = squadCount;
    }

    public int getLineupCount() {
        return lineupCount;
    }

    public void setLineupCount(int lineupCount) {
        this.lineupCount = lineupCount;
    }
}
