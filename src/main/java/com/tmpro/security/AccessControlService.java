package com.tmpro.security;

import com.tmpro.model.Match;
import com.tmpro.model.Team;
import com.tmpro.repository.TeamRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
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

    public List<String> getVisibleTeamIdsStr() {
        SecurityUser user = requireUser();
        if (currentUserService.isAdmin(user)) {
            return teamRepository.findAll().stream().map(Team::getId).collect(Collectors.toList());
        }
        if (currentUserService.isTrainer(user)) {
            return teamRepository.findByOwnerUserId(user.getId()).stream().map(Team::getId).collect(Collectors.toList());
        }
        if (user.getTeamId() != null && !user.getTeamId().isEmpty()) {
            return List.of(user.getTeamId());
        }
        return Collections.emptyList();
    }

    public void assertCanViewTeamStr(String teamId) {
        SecurityUser user = requireUser();
        if (currentUserService.isAdmin(user)) return;

        if (teamId == null || teamId.isEmpty()) {
            throw new AccessDeniedException("Equipo no especificado.");
        }
        if (!getVisibleTeamIdsStr().contains(teamId)) {
            throw new AccessDeniedException("No tienes acceso a este equipo.");
        }
    }

    public void assertCanManageTeamStr(String teamId) {
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
        SecurityUser user = requireUser();
        if (currentUserService.isAdmin(user)) return;

        if (match.getTeamId() == null || match.getTeamId().isEmpty()) {
            throw new AccessDeniedException("No tienes acceso a este partido.");
        }
        assertCanViewTeamStr(match.getTeamId());
    }

    public void assertCanManageMatch(Match match) {
        assertCanViewMatch(match);
        SecurityUser user = requireUser();
        if (currentUserService.isUser(user)) {
            throw new AccessDeniedException("Solo puedes consultar partidos en modo lectura.");
        }
        if (currentUserService.isTrainer(user) && match.getTeamId() != null && !match.getTeamId().isEmpty()) {
            assertCanManageTeamStr(match.getTeamId());
        }
    }
}
