package com.tmpro.service;

import com.tmpro.model.Match;
import com.tmpro.model.MatchStatus;
import com.tmpro.model.Player;
import com.tmpro.model.Statistic;
import com.tmpro.model.dto.LiveStatDTO;
import com.tmpro.model.dto.MatchDetailDTO;
import com.tmpro.model.dto.MatchScoreUpdateRequest;
import com.tmpro.model.dto.MatchSummaryDTO;
import com.tmpro.repository.MatchRepository;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.security.AccessControlService;
import com.tmpro.security.CurrentUserService;
import com.tmpro.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class MatchService {

    private static final int STARTING_ELEVEN = 11;
    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private StatisticRepository statisticRepository;

    @Autowired
    private AccessControlService accessControl;

    @Autowired
    private CurrentUserService currentUserService;

    public List<MatchSummaryDTO> findAllMatches() {
        List<String> teamIds = accessControl.getVisibleTeamIdsStr();
        SecurityUser user = accessControl.requireUser();

        List<Match> matches;
        if (currentUserService.isAdmin(user) && teamIds.isEmpty()) {
            matches = matchRepository.findAll(); // Sorting by date descending is done in repository
        } else if (teamIds.isEmpty()) {
            matches = List.of();
        } else {
            matches = matchRepository.findByTeamIdIn(teamIds); // Sort is handled in repo
        }

        return matches.stream()
                .map(m -> MatchSummaryDTO.from(
                        m,
                        (int) matchRepository.countSquadByMatchId(m.getId()),
                        (int) matchRepository.countLineupByMatchId(m.getId())
                ))
                .collect(Collectors.toList());
    }

    public MatchDetailDTO getMatchDetail(String id) {
        Match match = getMatchOrThrow(id);
        accessControl.assertCanViewMatch(match);
        return buildDetail(match);
    }

    public Match saveMatch(Match match) {
        SecurityUser user = accessControl.requireUser();
        if (!currentUserService.canManage(user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para crear partidos.");
        }
        if (match.getTeamId() == null) {
            throw new IllegalArgumentException("Debes asignar un equipo al partido.");
        }
        accessControl.assertCanManageTeamStr(match.getTeamId());
        if (match.getOpponentName() == null || match.getOpponentName().isBlank()) {
            match.setOpponentName("Rival");
        }
        if (match.getStatus() == null) {
            match.setStatus(MatchStatus.SCHEDULED);
        }
        match.setTeamScore(Math.max(0, match.getTeamScore()));
        match.setOpponentScore(Math.max(0, match.getOpponentScore()));
        return matchRepository.save(match);
    }

    public void deleteMatch(String id) {
        Match match = getMatchOrThrow(id);
        accessControl.assertCanManageMatch(match);
        matchRepository.deleteById(id);
    }

    public MatchDetailDTO closeMatch(String matchId) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        if (match.getStatus() == MatchStatus.FINISHED) {
            return getMatchDetail(matchId);
        }

        match.setStatus(MatchStatus.FINISHED);
        matchRepository.save(match);

        try {
            ensureLiveStats(match);
        } catch (Exception ex) {
            log.warn("No se pudieron sincronizar estadísticas al cerrar el partido {}", matchId, ex);
        }
        return getMatchDetail(matchId);
    }

    public MatchDetailDTO updateSquad(String matchId, List<String> playerIds) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);
        
        List<Player> players = resolvePlayers(playerIds, match.getTeamId());
        List<String> validIds = players.stream().map(Player::getId).collect(Collectors.toList());
        match.setSquadPlayerIds(new ArrayList<>(validIds));

        Set<String> squadIds = new HashSet<>(validIds);
        List<String> filteredLineup = match.getLineupPlayerIds().stream()
                .filter(squadIds::contains)
                .collect(Collectors.toList());
        match.setLineupPlayerIds(new ArrayList<>(filteredLineup));

        matchRepository.save(match);
        ensureLiveStats(match);
        return getMatchDetail(matchId);
    }

    public MatchDetailDTO updateLineup(String matchId, List<String> playerIds) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);

        if (playerIds.size() > STARTING_ELEVEN) {
            throw new IllegalArgumentException("El once inicial puede tener como máximo 11 jugadores.");
        }

        Set<String> squadIds = new HashSet<>(match.getSquadPlayerIds());
        if (squadIds.isEmpty()) {
            throw new IllegalArgumentException("Primero define la convocatoria del partido.");
        }

        for (String playerId : playerIds) {
            if (!squadIds.contains(playerId)) {
                throw new IllegalArgumentException("El jugador " + playerId + " no está en la convocatoria.");
            }
        }

        List<Player> players = resolvePlayers(playerIds, match.getTeamId());
        match.setLineupPlayerIds(players.stream().map(Player::getId).collect(Collectors.toList()));
        matchRepository.save(match);
        ensureLiveStats(match);
        return getMatchDetail(matchId);
    }

    public MatchDetailDTO updateScore(String matchId, MatchScoreUpdateRequest request) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);

        if (request.getTeamScore() != null) {
            match.setTeamScore(Math.max(0, request.getTeamScore()));
        }
        if (request.getOpponentScore() != null) {
            match.setOpponentScore(Math.max(0, request.getOpponentScore()));
        }
        if (request.getOpponentName() != null && !request.getOpponentName().isBlank()) {
            match.setOpponentName(request.getOpponentName().trim());
        }
        matchRepository.save(match);
        return getMatchDetail(matchId);
    }

    public List<LiveStatDTO> updateLiveStats(String matchId, List<LiveStatDTO> updates) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);
        Set<String> starterIds = new HashSet<>(match.getLineupPlayerIds());

        for (LiveStatDTO dto : updates) {
            Statistic stat = statisticRepository
                    .findByMatchEntityIdAndPlayerId(matchId, dto.getPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Estadística no encontrada para el jugador " + dto.getPlayerId()));

            stat.setGoals(Math.max(0, dto.getGoals()));
            stat.setAssists(Math.max(0, dto.getAssists()));
            stat.setMinutesPlayed(Math.min(120, Math.max(0, dto.getMinutesPlayed())));
            statisticRepository.save(stat);
        }

        return statisticRepository.findByMatchIdWithPlayer(matchId).stream()
                .map(s -> {
                    Player p = playerService.findById(s.getPlayerId()).orElse(new Player());
                    return new LiveStatDTO(s, p, starterIds.contains(s.getPlayerId()));
                })
                .collect(Collectors.toList());
    }

    private MatchDetailDTO buildDetail(Match match) {
        String id = match.getId();
        List<Player> squadPlayers = matchRepository.findSquadPlayersByMatchId(id);
        List<Player> lineupPlayers = matchRepository.findLineupPlayersByMatchId(id);

        Set<String> starterIds = lineupPlayers.stream()
                .filter(Objects::nonNull)
                .map(Player::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
        List<LiveStatDTO> stats = statisticRepository.findByMatchIdWithPlayer(id).stream()
                .filter(s -> s.getPlayerId() != null)
                .map(s -> {
                    Player p = squadPlayers.stream().filter(sp -> s.getPlayerId().equals(sp.getId())).findFirst().orElse(new Player());
                    return new LiveStatDTO(s, p, starterIds.contains(s.getPlayerId()));
                })
                .collect(Collectors.toList());
                
        // Necesitamos pasar squad y lineup players al DTO. El DTO usa Match, pero no tiene squad/lineup entities ya.
        // El DTO asume match.getSquad(). Lo arreglaremos enviando las listas explícitamente si DTO lo requiere,
        // pero vamos a arreglar MatchDetailDTO.
        return new MatchDetailDTO(match, stats, squadPlayers, lineupPlayers);
    }

    private void ensureLiveStats(Match match) {
        String label = buildMatchLabel(match);
        Set<String> squadIds = new HashSet<>(match.getSquadPlayerIds());
        
        List<Player> squadPlayers = matchRepository.findSquadPlayersByMatchId(match.getId());

        for (Player player : squadPlayers) {
            if (player == null || player.getId() == null) {
                continue;
            }
            statisticRepository.findByMatchEntityIdAndPlayerId(match.getId(), player.getId())
                    .orElseGet(() -> {
                        Statistic stat = new Statistic();
                        stat.setPlayerId(player.getId());
                        stat.setMatchId(match.getId());
                        stat.setMatch(label);
                        return statisticRepository.save(stat);
                    });
        }

        List<Statistic> all = statisticRepository.findByMatchIdWithPlayer(match.getId());
        for (Statistic stat : all) {
            if (stat.getPlayerId() == null) {
                statisticRepository.deleteById(stat.getId());
                continue;
            }
            if (!squadIds.contains(stat.getPlayerId())) {
                statisticRepository.deleteById(stat.getId());
            }
        }
    }

    private Match getMatchOrThrow(String matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
    }

    private void assertNotFinished(Match match) {
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalStateException("El partido está cerrado.");
        }
    }

    private List<Player> resolvePlayers(List<String> playerIds, String teamId) {
        List<Player> players = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : playerIds) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            Player player = playerService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + id));
            if (teamId != null && (player.getTeamId() == null || !teamId.equals(player.getTeamId()))) {
                throw new IllegalArgumentException("El jugador " + id + " no pertenece al equipo del partido.");
            }
            players.add(player);
        }
        return players;
    }

    private String buildMatchLabel(Match match) {
        String location = match.getLocation() != null ? match.getLocation() : "Partido";
        String date = match.getDate() != null ? String.valueOf(match.getDate()) : "";
        return date.isBlank() ? location : location + " · " + date;
    }
}
