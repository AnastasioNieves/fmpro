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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
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

    @Transactional(readOnly = true)
    public List<MatchSummaryDTO> findAllMatches() {
        List<Long> teamIds = accessControl.getVisibleTeamIds();
        SecurityUser user = accessControl.requireUser();

        List<Match> matches;
        if (currentUserService.isAdmin(user) && teamIds.isEmpty()) {
            matches = matchRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
        } else if (teamIds.isEmpty()) {
            matches = List.of();
        } else {
            matches = matchRepository.findByTeamIdIn(teamIds, Sort.by(Sort.Direction.DESC, "date"));
        }

        return matches.stream()
                .map(m -> MatchSummaryDTO.from(
                        m,
                        (int) matchRepository.countSquadByMatchId(m.getId()),
                        (int) matchRepository.countLineupByMatchId(m.getId())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchDetailDTO getMatchDetail(Long id) {
        Match match = getMatchOrThrow(id);
        accessControl.assertCanViewMatch(match);
        return buildDetail(match);
    }

    @Transactional
    public Match saveMatch(Match match) {
        SecurityUser user = accessControl.requireUser();
        if (!currentUserService.canManage(user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para crear partidos.");
        }
        if (match.getTeamId() == null) {
            throw new IllegalArgumentException("Debes asignar un equipo al partido.");
        }
        accessControl.assertCanManageTeam(match.getTeamId());
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

    @Transactional
    public void deleteMatch(Long id) {
        Match match = getMatchOrThrow(id);
        accessControl.assertCanManageMatch(match);
        matchRepository.deleteById(id);
    }

    @Transactional
    public MatchDetailDTO closeMatch(Long matchId) {
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

    @Transactional
    public MatchDetailDTO updateSquad(Long matchId, List<Long> playerIds) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);
        List<Player> players = resolvePlayers(playerIds, match.getTeamId());
        match.setSquad(new ArrayList<>(players));

        Set<Long> squadIds = players.stream().map(Player::getId).collect(Collectors.toSet());
        List<Player> filteredLineup = match.getLineup().stream()
                .filter(p -> squadIds.contains(p.getId()))
                .collect(Collectors.toList());
        match.setLineup(new ArrayList<>(filteredLineup));

        matchRepository.save(match);
        ensureLiveStats(match);
        return getMatchDetail(matchId);
    }

    @Transactional
    public MatchDetailDTO updateLineup(Long matchId, List<Long> playerIds) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);

        if (playerIds.size() > STARTING_ELEVEN) {
            throw new IllegalArgumentException("El once inicial puede tener como máximo 11 jugadores.");
        }

        Set<Long> squadIds = match.getSquad().stream().map(Player::getId).collect(Collectors.toSet());
        if (squadIds.isEmpty()) {
            throw new IllegalArgumentException("Primero define la convocatoria del partido.");
        }

        for (Long playerId : playerIds) {
            if (!squadIds.contains(playerId)) {
                throw new IllegalArgumentException("El jugador " + playerId + " no está en la convocatoria.");
            }
        }

        List<Player> players = resolvePlayers(playerIds, match.getTeamId());
        match.setLineup(new ArrayList<>(players));
        matchRepository.save(match);
        ensureLiveStats(match);
        return getMatchDetail(matchId);
    }

    @Transactional
    public MatchDetailDTO updateScore(Long matchId, MatchScoreUpdateRequest request) {
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

    @Transactional
    public List<LiveStatDTO> updateLiveStats(Long matchId, List<LiveStatDTO> updates) {
        Match match = getMatchOrThrow(matchId);
        accessControl.assertCanManageMatch(match);
        assertNotFinished(match);
        Set<Long> starterIds = match.getLineup().stream().map(Player::getId).collect(Collectors.toSet());

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
                .map(s -> new LiveStatDTO(s, starterIds.contains(s.getPlayer().getId())))
                .collect(Collectors.toList());
    }

    private MatchDetailDTO buildDetail(Match match) {
        Long id = match.getId();
        match.setSquad(matchRepository.findSquadPlayersByMatchId(id));
        match.setLineup(matchRepository.findLineupPlayersByMatchId(id));

        Set<Long> starterIds = match.getLineup().stream()
                .filter(Objects::nonNull)
                .map(Player::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<LiveStatDTO> stats = statisticRepository.findByMatchIdWithPlayer(id).stream()
                .filter(s -> s.getPlayer() != null)
                .map(s -> new LiveStatDTO(s, starterIds.contains(s.getPlayer().getId())))
                .collect(Collectors.toList());
        return new MatchDetailDTO(match, stats);
    }

    private void ensureLiveStats(Match match) {
        String label = buildMatchLabel(match);
        Set<Long> squadIds = match.getSquad().stream()
                .filter(Objects::nonNull)
                .map(Player::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        for (Player player : match.getSquad()) {
            if (player == null || player.getId() == null) {
                continue;
            }
            statisticRepository.findByMatchEntityIdAndPlayerId(match.getId(), player.getId())
                    .orElseGet(() -> {
                        Statistic stat = new Statistic();
                        stat.setPlayer(player);
                        stat.setMatchEntity(match);
                        stat.setMatch(label);
                        return statisticRepository.save(stat);
                    });
        }

        List<Statistic> all = statisticRepository.findByMatchIdWithPlayer(match.getId());
        for (Statistic stat : all) {
            if (stat.getPlayer() == null || stat.getPlayer().getId() == null) {
                statisticRepository.delete(stat);
                continue;
            }
            if (!squadIds.contains(stat.getPlayer().getId())) {
                statisticRepository.delete(stat);
            }
        }
    }

    private Match getMatchOrThrow(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        match.setSquad(matchRepository.findSquadPlayersByMatchId(matchId));
        match.setLineup(matchRepository.findLineupPlayersByMatchId(matchId));
        return match;
    }

    private void assertNotFinished(Match match) {
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalStateException("El partido está cerrado.");
        }
    }

    private List<Player> resolvePlayers(List<Long> playerIds, Long teamId) {
        List<Player> players = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : playerIds) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            Player player = playerService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + id));
            if (teamId != null && (player.getTeam() == null || !teamId.equals(player.getTeam().getId()))) {
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

