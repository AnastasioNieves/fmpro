package com.tmpro.controller;

import com.tmpro.model.Player;
import com.tmpro.model.Statistic;
import com.tmpro.model.StatisticDTO;
import com.tmpro.model.dto.StatisticsSummaryDTO;
import com.tmpro.service.PlayerService;
import com.tmpro.service.StatisticService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {

    private final StatisticService statisticService;
    private final PlayerService playerService;

    public StatisticController(StatisticService statisticService, PlayerService playerService) {
        this.statisticService = statisticService;
        this.playerService = playerService;
    }

    private StatisticDTO toDTO(Statistic stat) {
        Player p = null;
        if (stat.getPlayerId() != null) {
            p = playerService.findById(stat.getPlayerId()).orElse(null);
        }
        return new StatisticDTO(stat, p);
    }

    @PostMapping
    public ResponseEntity<Statistic> createStatistic(@RequestBody Statistic statistic) {
        Statistic createdStatistic = statisticService.createStatistic(statistic);
        return ResponseEntity.ok(createdStatistic);
    }

    @GetMapping("/summary")
    public ResponseEntity<StatisticsSummaryDTO> getSummary() {
        return ResponseEntity.ok(statisticService.getSummary());
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<StatisticDTO>> getStatisticsByTeamId(@PathVariable String teamId) {
        List<StatisticDTO> statisticDTOs = statisticService.getStatisticsDTOByTeamId(teamId);
        return ResponseEntity.ok(statisticDTOs);
    }

    @GetMapping
    public ResponseEntity<List<StatisticDTO>> getAllStatistics() {
        List<Statistic> statistics = statisticService.getAllStatistic();
        List<StatisticDTO> statisticDTOs = statistics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statisticDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatisticDTO> getStatisticById(@PathVariable String id) {
        Optional<Statistic> statistic = statisticService.getStatisticById(id);
        return statistic.map(stat -> ResponseEntity.ok(toDTO(stat)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<StatisticDTO>> getStatisticsByPlayerId(@PathVariable String playerId) {
        List<Statistic> statistics = statisticService.getStatisticsByPlayerId(playerId);
        List<StatisticDTO> statisticDTOs = statistics.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statisticDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatisticDTO> updateStatistic(@PathVariable String id, @RequestBody Statistic updatedStatistic) {
        try {
            Statistic updated = statisticService.updateStatistic(id, updatedStatistic);
            return ResponseEntity.ok(toDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatistic(@PathVariable String id) {
        boolean isDeleted = statisticService.deleteStatistic(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
