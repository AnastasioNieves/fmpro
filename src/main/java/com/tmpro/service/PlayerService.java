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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@SuppressWarnings("null")
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

    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        List<Long> teamIds = accessControl.getVisibleTeamIds();
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return playerRepository.findAllWithTeamByTeamIdIn(teamIds);
    }

    public Optional<Player> findByName(String name) {
        return playerRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id).filter(this::canViewPlayer);
    }

    @Transactional(readOnly = true)
    public Optional<Player> findByIdWithTeam(Long id) {
        return playerRepository.findByIdWithTeam(id).filter(this::canViewPlayer);
    }

    @Transactional
    public Player createPlayer(Player player) {
        assertCanManagePlayerTeam(player);
        return playerRepository.save(player);
    }

    @Transactional
    public Optional<Player> updatePlayer(Long id, Player updatedPlayer) {
        return playerRepository.findById(id).map(existingPlayer -> {
            assertCanManagePlayerTeam(updatedPlayer);
            existingPlayer.setName(updatedPlayer.getName());
            existingPlayer.setPosition(updatedPlayer.getPosition());
            existingPlayer.setDorsal(updatedPlayer.getDorsal());
            existingPlayer.setTeam(updatedPlayer.getTeam());
            return playerRepository.save(existingPlayer);
        });
    }

    @Transactional
    public boolean deletePlayer(Long id) {
        return playerRepository.findById(id).map(player -> {
            if (player.getTeam() != null) {
                accessControl.assertCanManageTeam(player.getTeam().getId());
            }
            playerRepository.delete(player);
            return true;
        }).orElse(false);
    }

    public List<Statistic> getPlayerStatistics(Long id) {
        Player player = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
        return statisticRepository.findByPlayerId(player.getId());
    }

    public Long findPlayerIdByName(String name) {
        return playerRepository.findIdByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + name));
    }

    private boolean canViewPlayer(Player player) {
        if (player.getTeam() == null) {
            return false;
        }
        try {
            accessControl.assertCanViewTeam(player.getTeam().getId());
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
        if (player.getTeam() == null || player.getTeam().getId() == null) {
            throw new IllegalArgumentException("team_id es obligatorio");
        }
        accessControl.assertCanManageTeam(player.getTeam().getId());
    }
}

