package com.tmpro.repository;

import com.tmpro.model.Player;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class PlayerRepository {

    private final Map<String, Player> data = new ConcurrentHashMap<>();

    public Player save(Player player) {
        if (player.getId() == null || player.getId().isEmpty()) {
            player.setId(UUID.randomUUID().toString());
        }
        data.put(player.getId(), player);
        return player;
    }

    public Optional<Player> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Player> findAll() {
        return new ArrayList<>(data.values());
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public Optional<Player> findByName(String name) {
        return data.values().stream()
                .filter(p -> p.getName() != null && p.getName().equals(name))
                .findFirst();
    }

    public Optional<String> findIdByName(String name) {
        return findByName(name).map(Player::getId);
    }

    public List<Player> findAllWithTeam() {
        return findAll();
    }

    public List<Player> findAllWithTeamByTeamIdIn(List<String> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) return new ArrayList<>();
        return data.values().stream()
                .filter(p -> teamIds.contains(p.getTeamId()))
                .collect(Collectors.toList());
    }

    public Optional<Player> findByIdWithTeam(String id) {
        return findById(id);
    }

    public long countByTeamId(String teamId) {
        return findByTeamId(teamId).size();
    }

    public List<Player> findByTeamId(String teamId) {
        return data.values().stream()
                .filter(p -> p.getTeamId() != null && p.getTeamId().equals(teamId))
                .collect(Collectors.toList());
    }
}
