package com.tmpro.model.dto;

import com.tmpro.model.Match;
import com.tmpro.model.MatchStatus;
import com.tmpro.model.Player;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MatchDetailDTO {

    private String id;
    private Date date;
    private String location;
    private String teamId;
    private String opponentName;
    private MatchStatus status;
    private boolean home;
    private int teamScore;
    private int opponentScore;
    private List<PlayerSummaryDTO> squad;
    private List<PlayerSummaryDTO> lineup;
    private List<LiveStatDTO> liveStats;

    public MatchDetailDTO() {
    }

    public MatchDetailDTO(Match match, List<LiveStatDTO> liveStats, List<Player> squadPlayers, List<Player> lineupPlayers) {
        this.id = match.getId();
        this.date = match.getDate();
        this.location = match.getLocation();
        this.teamId = match.getTeamId();
        this.opponentName = match.getOpponentName();
        this.status = match.getStatus();
        this.home = match.isHome();
        this.teamScore = match.getTeamScore();
        this.opponentScore = match.getOpponentScore();
        this.squad = squadPlayers.stream().map(PlayerSummaryDTO::new).collect(Collectors.toList());
        this.lineup = lineupPlayers.stream().map(PlayerSummaryDTO::new).collect(Collectors.toList());
        this.liveStats = liveStats;
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

    public List<PlayerSummaryDTO> getSquad() {
        return squad;
    }

    public void setSquad(List<PlayerSummaryDTO> squad) {
        this.squad = squad;
    }

    public List<PlayerSummaryDTO> getLineup() {
        return lineup;
    }

    public void setLineup(List<PlayerSummaryDTO> lineup) {
        this.lineup = lineup;
    }

    public List<LiveStatDTO> getLiveStats() {
        return liveStats;
    }

    public void setLiveStats(List<LiveStatDTO> liveStats) {
        this.liveStats = liveStats;
    }
}
