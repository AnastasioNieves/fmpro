package com.tmpro.service;

import com.tmpro.model.Statistic;
import com.tmpro.model.Player;
import com.tmpro.model.dto.StatisticsSummaryDTO;
import com.tmpro.model.StatisticDTO;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("all")
public class StatisticService {

    @Autowired
    private StatisticRepository statisticRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AccessControlService accessControl;

    // Crear una nueva estadística
    public Statistic createStatistic(Statistic statistic) {
        if (statistic.getPlayerId() == null || statistic.getPlayerId().isEmpty()) {
            throw new IllegalArgumentException("El jugador o su ID no pueden ser nulos");
        }

        Player player = playerRepository.findById(statistic.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        statistic.setPlayerId(player.getId());
        return statisticRepository.save(statistic);
    }

    public List<Statistic> getAllStatistic() {
        return statisticRepository.findAllWithPlayer();
    }

    public Optional<Statistic> getStatisticById(String id) {
        return statisticRepository.findByIdWithPlayer(id);
    }

    public List<Statistic> getStatisticsByPlayerId(String playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        if (player.getTeamId() != null) {
            accessControl.assertCanViewTeamStr(player.getTeamId());
        }
        return statisticRepository.findByPlayerIdWithPlayer(playerId);
    }

    public List<Statistic> getStatisticsByTeamId(String teamId) {
        accessControl.assertCanViewTeamStr(teamId);
        List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
        List<String> playerIds = teamPlayers.stream().map(Player::getId).collect(java.util.stream.Collectors.toList());
        return statisticRepository.findByPlayerIds(playerIds);
    }

    public List<StatisticDTO> getStatisticsDTOByTeamId(String teamId) {
        accessControl.assertCanViewTeamStr(teamId);
        List<Player> teamPlayers = playerRepository.findByTeamId(teamId);
        java.util.Map<String, Player> playerMap = teamPlayers.stream().collect(java.util.stream.Collectors.toMap(Player::getId, p -> p));
        
        List<String> playerIds = teamPlayers.stream().map(Player::getId).collect(java.util.stream.Collectors.toList());
        List<Statistic> stats = statisticRepository.findByPlayerIds(playerIds);
        
        return stats.stream()
                .map(s -> new StatisticDTO(s, playerMap.get(s.getPlayerId())))
                .collect(java.util.stream.Collectors.toList());
    }

    public StatisticsSummaryDTO getSummary() {
        List<Object[]> results = statisticRepository.getAggregatedSummary();
        if (results == null || results.isEmpty() || results.get(0) == null) {
            return new StatisticsSummaryDTO(0, 0, 0);
        }
        Object[] row = results.get(0);
        int count = row[0] != null ? ((Number) row[0]).intValue() : 0;
        int goals = row[1] != null ? ((Number) row[1]).intValue() : 0;
        int assists = row[2] != null ? ((Number) row[2]).intValue() : 0;
        return new StatisticsSummaryDTO(count, goals, assists);
    }

    public Statistic updateStatistic(String id, Statistic updatedStatistic) {
        Statistic saved = statisticRepository.findById(id)
                .map(statistic -> {
                    statistic.setGoals(updatedStatistic.getGoals());
                    statistic.setAssists(updatedStatistic.getAssists());
                    statistic.setMinutesPlayed(updatedStatistic.getMinutesPlayed());
                    statistic.setMatch(updatedStatistic.getMatch());
                    statistic.setShotsTotal(updatedStatistic.getShotsTotal());
                    statistic.setShotsOnTarget(updatedStatistic.getShotsOnTarget());
                    statistic.setPassesTotal(updatedStatistic.getPassesTotal());
                    statistic.setPassesCompleted(updatedStatistic.getPassesCompleted());
                    statistic.setDuelsTotal(updatedStatistic.getDuelsTotal());
                    statistic.setDuelsWon(updatedStatistic.getDuelsWon());
                    statistic.setInterceptions(updatedStatistic.getInterceptions());
                    statistic.setSaves(updatedStatistic.getSaves());
                    statistic.setGoalsConceded(updatedStatistic.getGoalsConceded());
                    return statisticRepository.save(statistic);
                })
                .orElseThrow(() -> new RuntimeException("Estadística no encontrada"));
        return statisticRepository.findByIdWithPlayer(saved.getId()).orElse(saved);
    }

    // Eliminar una estadística
    public boolean deleteStatistic(String id) {
        if (statisticRepository.findById(id).isPresent()) {
            statisticRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
