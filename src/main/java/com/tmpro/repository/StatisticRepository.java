package com.tmpro.repository;

import com.tmpro.model.Statistic;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class StatisticRepository {

    private final Map<String, Statistic> data = new ConcurrentHashMap<>();

    public Statistic save(Statistic statistic) {
        if (statistic.getId() == null || statistic.getId().isEmpty()) {
            statistic.setId(UUID.randomUUID().toString());
        }
        data.put(statistic.getId(), statistic);
        return statistic;
    }

    public Optional<Statistic> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Statistic> findAll() {
        return new ArrayList<>(data.values());
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public List<Statistic> findByPlayerId(String playerId) {
        return data.values().stream()
                .filter(s -> s.getPlayerId() != null && s.getPlayerId().equals(playerId))
                .collect(Collectors.toList());
    }

    public List<Statistic> findAllWithPlayer() {
        return findAll();
    }

    public Optional<Statistic> findByIdWithPlayer(String id) {
        return findById(id);
    }

    public List<Statistic> findByPlayerIdWithPlayer(String playerId) {
        return findByPlayerId(playerId);
    }

    public List<Statistic> findByTeamIdWithPlayer(String teamId) {
        return new ArrayList<>();
    }

    public List<Statistic> findByMatchIdWithPlayer(String matchId) {
        return data.values().stream()
                .filter(s -> s.getMatchId() != null && s.getMatchId().equals(matchId))
                .collect(Collectors.toList());
    }

    public List<Statistic> findByPlayerIds(List<String> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) return new ArrayList<>();
        return data.values().stream()
                .filter(s -> playerIds.contains(s.getPlayerId()))
                .collect(Collectors.toList());
    }

    public Optional<Statistic> findByMatchEntityIdAndPlayerId(String matchId, String playerId) {
        return data.values().stream()
                .filter(s -> s.getMatchId() != null && s.getMatchId().equals(matchId) &&
                             s.getPlayerId() != null && s.getPlayerId().equals(playerId))
                .findFirst();
    }

    public List<Object[]> getAggregatedSummary() {
        long count = data.size();
        long sumGoals = data.values().stream().mapToLong(s -> s.getGoals()).sum();
        long sumAssists = data.values().stream().mapToLong(s -> s.getAssists()).sum();
        
        List<Object[]> res = new ArrayList<>();
        res.add(new Object[]{count, sumGoals, sumAssists});
        return res;
    }
}
