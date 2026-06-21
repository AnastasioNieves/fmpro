package com.tmpro.repository;

import com.tmpro.model.Match;
import com.tmpro.model.Player;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class MatchRepository {

    private final Map<String, Match> data = new ConcurrentHashMap<>();

    @Autowired
    private PlayerRepository playerRepository;

    public Match save(Match match) {
        if (match.getId() == null || match.getId().isEmpty()) {
            match.setId(UUID.randomUUID().toString());
        }
        data.put(match.getId(), match);
        return match;
    }

    public Optional<Match> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Match> findAll() {
        return data.values().stream()
                .sorted((a, b) -> {
                    if (a.getDate() != null && b.getDate() != null) {
                        return b.getDate().compareTo(a.getDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public List<Match> findByTeamIdIn(List<String> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return new ArrayList<>();
        return data.values().stream()
                .filter(m -> teamIds.contains(m.getTeamId()))
                .sorted((a, b) -> {
                    if (a.getDate() != null && b.getDate() != null) {
                        return b.getDate().compareTo(a.getDate());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    public List<Player> findSquadPlayersByMatchId(String matchId) {
        Match match = findById(matchId).orElse(null);
        if (match == null || match.getSquadPlayerIds() == null || match.getSquadPlayerIds().isEmpty()) return new ArrayList<>();
        List<Player> players = new ArrayList<>();
        for (String pid : match.getSquadPlayerIds()) {
            playerRepository.findById(pid).ifPresent(players::add);
        }
        return players.stream().sorted(Comparator.comparing(p -> {
            try { return Integer.parseInt(p.getDorsal()); } catch (Exception e) { return 999; }
        })).collect(Collectors.toList());
    }

    public List<Player> findLineupPlayersByMatchId(String matchId) {
        Match match = findById(matchId).orElse(null);
        if (match == null || match.getLineupPlayerIds() == null || match.getLineupPlayerIds().isEmpty()) return new ArrayList<>();
        List<Player> players = new ArrayList<>();
        for (String pid : match.getLineupPlayerIds()) {
            playerRepository.findById(pid).ifPresent(players::add);
        }
        return players.stream().sorted(Comparator.comparing(p -> {
            try { return Integer.parseInt(p.getDorsal()); } catch (Exception e) { return 999; }
        })).collect(Collectors.toList());
    }

    public long countSquadByMatchId(String matchId) {
        Match match = findById(matchId).orElse(null);
        return match != null && match.getSquadPlayerIds() != null ? match.getSquadPlayerIds().size() : 0;
    }

    public long countLineupByMatchId(String matchId) {
        Match match = findById(matchId).orElse(null);
        return match != null && match.getLineupPlayerIds() != null ? match.getLineupPlayerIds().size() : 0;
    }
}
