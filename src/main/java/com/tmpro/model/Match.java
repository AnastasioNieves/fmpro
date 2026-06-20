package com.tmpro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;
    private String location;

    @Column(name = "team_id")
    private Long teamId;

    private String opponentName = "Rival";

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "is_home")
    private Boolean home = true;

    @Column(name = "team_score")
    private Integer teamScore = 0;

    @Column(name = "opponent_score")
    private Integer opponentScore = 0;

    @ManyToMany
    @JoinTable(
            name = "match_squad",
            joinColumns = @JoinColumn(name = "match_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @JsonIgnore
    @JsonIgnoreProperties({"team", "hibernateLazyInitializer", "handler"})
    private List<Player> squad = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "match_lineup",
            joinColumns = @JoinColumn(name = "match_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @JsonIgnore
    @JsonIgnoreProperties({"team", "hibernateLazyInitializer", "handler"})
    private List<Player> lineup = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
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

    public List<Player> getSquad() {
        return squad;
    }

    public void setSquad(List<Player> squad) {
        this.squad = squad != null ? squad : new ArrayList<>();
    }

    public List<Player> getLineup() {
        return lineup;
    }

    public void setLineup(List<Player> lineup) {
        this.lineup = lineup != null ? lineup : new ArrayList<>();
    }
}
