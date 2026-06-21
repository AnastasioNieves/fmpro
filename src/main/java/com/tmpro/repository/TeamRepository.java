package com.tmpro.repository;

import com.tmpro.model.Team;
import com.tmpro.model.TeamDTO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TeamRepository {

    private final Map<String, Team> data = new ConcurrentHashMap<>();

    public Team save(Team team) {
        if (team.getId() == null || team.getId().isEmpty()) {
            team.setId(UUID.randomUUID().toString());
        }
        data.put(team.getId(), team);
        return team;
    }

    public Optional<Team> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Team> findAll() {
        return new ArrayList<>(data.values());
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public Team findByName(String name) {
        return data.values().stream()
                .filter(t -> t.getName() != null && t.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<Team> findByOwnerUserId(String ownerUserId) {
        return data.values().stream()
                .filter(t -> t.getOwnerUserId() != null && t.getOwnerUserId().equals(ownerUserId))
                .collect(Collectors.toList());
    }

    public List<Team> findByIdIn(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return data.values().stream()
                .filter(t -> ids.contains(t.getId()))
                .collect(Collectors.toList());
    }

    public List<TeamDTO> findAllAsDTO() {
        return data.values().stream()
                .map(t -> new TeamDTO(t.getId(), t.getName(), t.getCoach()))
                .collect(Collectors.toList());
    }
}
