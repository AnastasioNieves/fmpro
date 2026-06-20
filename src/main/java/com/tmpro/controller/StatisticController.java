package com.tmpro.controller;

import com.tmpro.model.Statistic;
import com.tmpro.model.StatisticDTO;
import com.tmpro.model.dto.StatisticsSummaryDTO;
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

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    // Crear una nueva estadística
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
    public ResponseEntity<List<StatisticDTO>> getStatisticsByTeamId(@PathVariable Long teamId) {
        List<StatisticDTO> statisticDTOs = statisticService.getStatisticsByTeamId(teamId).stream()
                .map(StatisticDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statisticDTOs);
    }

    // Obtener estadísticas (listado completo; preferir filtros por equipo/jugador)
    @GetMapping
    public ResponseEntity<List<StatisticDTO>> getAllStatistics() {
        List<Statistic> statistics = statisticService.getAllStatistic();
        List<StatisticDTO> statisticDTOs = statistics.stream()
                .map(StatisticDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statisticDTOs);
    }

    // Obtener estadística por ID
    @GetMapping("/{id}")
    public ResponseEntity<StatisticDTO> getStatisticById(@PathVariable Long id) {
        Optional<Statistic> statistic = statisticService.getStatisticById(id);
        return statistic.map(stat -> ResponseEntity.ok(new StatisticDTO(stat)))  // Convertir a StatisticDTO
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Obtener todas las estadísticas de un jugador
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<StatisticDTO>> getStatisticsByPlayerId(@PathVariable Long playerId) {
        List<Statistic> statistics = statisticService.getStatisticsByPlayerId(playerId);
        List<StatisticDTO> statisticDTOs = statistics.stream()
                .map(StatisticDTO::new)  // Convertir cada Statistic a StatisticDTO
                .collect(Collectors.toList());
        return ResponseEntity.ok(statisticDTOs);
    }

    // Actualizar una estadística
    @PutMapping("/{id}")
    public ResponseEntity<StatisticDTO> updateStatistic(@PathVariable Long id, @RequestBody Statistic updatedStatistic) {
        try {
            Statistic updated = statisticService.updateStatistic(id, updatedStatistic);
            return ResponseEntity.ok(new StatisticDTO(updated));  // Convertir a StatisticDTO
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();  // Si no se encuentra la estadística
        }
    }

    // Eliminar una estadística
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatistic(@PathVariable Long id) {
        boolean isDeleted = statisticService.deleteStatistic(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
