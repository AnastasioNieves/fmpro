package com.tmpro.service;

import com.tmpro.model.Player;
import com.tmpro.model.Statistic;
import com.tmpro.repository.PlayerRepository;
import com.tmpro.repository.StatisticRepository;
import com.tmpro.security.AccessControlService;
import com.tmpro.security.CurrentUserService;
import com.tmpro.security.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("all")
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final StatisticRepository statisticRepository;
    private final AccessControlService accessControl;
    private final CurrentUserService currentUserService;

    @Autowired
    public PlayerService(
            PlayerRepository playerRepository,
            StatisticRepository statisticRepository,
            AccessControlService accessControl,
            CurrentUserService currentUserService
    ) {
        this.playerRepository = playerRepository;
        this.statisticRepository = statisticRepository;
        this.accessControl = accessControl;
        this.currentUserService = currentUserService;
    }

    public List<Player> getAllPlayers() {
        // AccessControlService needs to be refactored to return List<String>
        List<String> teamIds = accessControl.getVisibleTeamIdsStr();
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return playerRepository.findAllWithTeamByTeamIdIn(teamIds);
    }

    public Optional<Player> findByName(String name) {
        return playerRepository.findByName(name);
    }

    public Optional<Player> findById(String id) {
        return playerRepository.findById(id).filter(this::canViewPlayer);
    }

    public Optional<Player> findByIdWithTeam(String id) {
        return playerRepository.findByIdWithTeam(id).filter(this::canViewPlayer);
    }

    public Player createPlayer(Player player) {
        assertCanManagePlayerTeam(player);
        return playerRepository.save(player);
    }

    public Optional<Player> updatePlayer(String id, Player updatedPlayer) {
        return playerRepository.findById(id).map(existingPlayer -> {
            assertCanManagePlayerTeam(updatedPlayer);
            existingPlayer.setName(updatedPlayer.getName());
            existingPlayer.setPosition(updatedPlayer.getPosition());
            existingPlayer.setDorsal(updatedPlayer.getDorsal());
            existingPlayer.setTeamId(updatedPlayer.getTeamId());
            return playerRepository.save(existingPlayer);
        });
    }

    public boolean deletePlayer(String id) {
        return playerRepository.findById(id).map(player -> {
            if (player.getTeamId() != null) {
                accessControl.assertCanManageTeamStr(player.getTeamId());
            }
            playerRepository.deleteById(player.getId());
            return true;
        }).orElse(false);
    }

    public List<Statistic> getPlayerStatistics(String id) {
        Player player = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        return statisticRepository.findByPlayerId(player.getId());
    }

    public String findPlayerIdByName(String name) {
        return playerRepository.findIdByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + name));
    }

    private boolean canViewPlayer(Player player) {
        if (player.getTeamId() == null) {
            return false;
        }
        try {
            accessControl.assertCanViewTeamStr(player.getTeamId());
            return true;
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return false;
        }
    }

    private void assertCanManagePlayerTeam(Player player) {
        SecurityUser user = accessControl.requireUser();
        if (!currentUserService.canManage(user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para gestionar jugadores.");
        }
        if (player.getTeamId() == null || player.getTeamId().isEmpty()) {
            throw new IllegalArgumentException("team_id es obligatorio");
        }
        accessControl.assertCanManageTeamStr(player.getTeamId());
    }
}
