package com.tmpro.service;

import com.tmpro.model.Statistic;
import com.tmpro.model.Player;
import com.tmpro.model.dto.StatisticsSummaryDTO;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.security.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class StatisticService {

    @Autowired
    private StatisticRepository statisticRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AccessControlService accessControl;

    // Crear una nueva estadística
    public Statistic createStatistic(Statistic statistic) {
        if (statistic.getPlayer() == null || statistic.getPlayer().getId() == null) {
            throw new IllegalArgumentException("El jugador o su ID no pueden ser nulos");
        }

        Player player = playerRepository.findById(statistic.getPlayer().getId())
                .orElseThrow(() -> new RuntimeException("Jugador no encontrado"));

        statistic.setPlayer(player);
        return statisticRepository.save(statistic);
    }


    @Transactional(readOnly = true)
    public List<Statistic> getAllStatistic() {
        return statisticRepository.findAllWithPlayer();
    }

    @Transactional(readOnly = true)
    public Optional<Statistic> getStatisticById(Long id) {
        return statisticRepository.findByIdWithPlayer(id);
    }

    @Transactional(readOnly = true)
    public List<Statistic> getStatisticsByPlayerId(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        if (player.getTeam() != null) {
            accessControl.assertCanViewTeam(player.getTeam().getId());
        }
        return statisticRepository.findByPlayerIdWithPlayer(playerId);
    }

    @Transactional(readOnly = true)
    public List<Statistic> getStatisticsByTeamId(Long teamId) {
        accessControl.assertCanViewTeam(teamId);
        return statisticRepository.findByTeamIdWithPlayer(teamId);
    }

    @Transactional(readOnly = true)
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

    @Transactional
    public Statistic updateStatistic(Long id, Statistic updatedStatistic) {
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
    public boolean deleteStatistic(Long id) {
        if (statisticRepository.existsById(id)) {
            statisticRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

