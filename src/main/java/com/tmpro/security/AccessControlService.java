package com.tmpro.security;

import com.tmpro.model.Match;
import com.tmpro.model.Team;
import com.tmpro.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@SuppressWarnings("null")
public class AccessControlService {

    private final CurrentUserService currentUserService;
    private final TeamRepository teamRepository;

    public AccessControlService(CurrentUserService currentUserService, TeamRepository teamRepository) {
        this.currentUserService = currentUserService;
        this.teamRepository = teamRepository;
    }

    public SecurityUser requireUser() {
        return currentUserService.requireUser();
    }

    public List<Long> getVisibleTeamIds() {
        SecurityUser user = requireUser();
        if (currentUserService.isAdmin(user)) {
            return teamRepository.findAll().stream().map(Team::getId).toList();
        }
        if (currentUserService.isTrainer(user)) {
            return teamRepository.findByOwnerUserId(user.getId()).stream().map(Team::getId).toList();
        }
        if (user.getTeamId() != null) {
            return List.of(user.getTeamId());
        }
        return Collections.emptyList();
    }

    public void assertCanViewTeam(Long teamId) {
        if (teamId == null) {
            throw new AccessDeniedException("Equipo no especificado.");
        }
        if (!getVisibleTeamIds().contains(teamId)) {
            throw new AccessDeniedException("No tienes acceso a este equipo.");
        }
    }

    public void assertCanManageTeam(Long teamId) {
        SecurityUser user = requireUser();
        if (currentUserService.isAdmin(user)) {
            return;
        }
        if (!currentUserService.isTrainer(user)) {
            throw new AccessDeniedException("No tienes permiso para gestionar equipos.");
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        if (!Objects.equals(team.getOwnerUserId(), user.getId())) {
            throw new AccessDeniedException("Solo puedes gestionar equipos que hayas creado.");
        }
    }

    public void assertCanViewMatch(Match match) {
        if (match.getTeamId() == null) {
            SecurityUser user = requireUser();
            if (currentUserService.isAdmin(user)) {
                return;
            }
            throw new AccessDeniedException("No tienes acceso a este partido.");
        }
        assertCanViewTeam(match.getTeamId());
    }

    public void assertCanManageMatch(Match match) {
        assertCanViewMatch(match);
        SecurityUser user = requireUser();
        if (currentUserService.isUser(user)) {
            throw new AccessDeniedException("Solo puedes consultar partidos en modo lectura.");
        }
        if (currentUserService.isTrainer(user) && match.getTeamId() != null) {
            assertCanManageTeam(match.getTeamId());
        }
    }
}

