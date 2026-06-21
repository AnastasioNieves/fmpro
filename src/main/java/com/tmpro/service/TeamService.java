package com.tmpro.service;

import com.tmpro.model.Team;
import com.tmpro.model.TeamDTO;
import com.tmpro.repository.TeamRepository;
import com.tmpro.security.AccessControlService;
import com.tmpro.security.CurrentUserService;
import com.tmpro.security.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessControlService accessControl;

    @Autowired
    private CurrentUserService currentUserService;

    public List<TeamDTO> getPublicTeams() {
        return teamRepository.findAllAsDTO();
    }

    public List<TeamDTO> getAllTeams() {
        List<String> visible = accessControl.getVisibleTeamIdsStr();
        if (visible.isEmpty()) {
            return List.of();
        }
        return teamRepository.findByIdIn(visible).stream()
                .map(t -> new TeamDTO(t.getId(), t.getName(), t.getCoach()))
                .collect(Collectors.toList());
    }

    public Team getTeamById(String id) {
        accessControl.assertCanViewTeamStr(id);
        return teamRepository.findById(id).orElse(null);
    }

    public Team saveTeam(Team team) {
        SecurityUser user = accessControl.requireUser();
        if (!currentUserService.canManage(user)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para crear equipos.");
        }
        if (currentUserService.isTrainer(user)) {
            team.setOwnerUserId(user.getId());
        } else if (currentUserService.isAdmin(user) && team.getOwnerUserId() == null) {
            team.setOwnerUserId(user.getId());
        }
        return teamRepository.save(team);
    }

    public Team updateTeam(String id, Team team) {
        accessControl.assertCanManageTeamStr(id);
        team.setId(id);
        Team existing = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        team.setOwnerUserId(existing.getOwnerUserId());
        return teamRepository.save(team);
    }

    public boolean deleteTeam(String id) {
        if (teamRepository.findById(id).isEmpty()) {
            return false;
        }
        accessControl.assertCanManageTeamStr(id);
        teamRepository.deleteById(id);
        return true;
    }

    public Optional<Team> findById(String id) {
        return teamRepository.findById(id);
    }
}
