package com.tmpro.service;

import com.tmpro.model.Team;
import com.tmpro.model.TeamDTO;
import com.tmpro.repository.TeamRepository;
import com.tmpro.security.AccessControlService;
import com.tmpro.security.CurrentUserService;
import com.tmpro.security.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessControlService accessControl;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<TeamDTO> getPublicTeams() {
        return teamRepository.findAllAsDTO();
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeams() {
        List<Long> visible = accessControl.getVisibleTeamIds();
        if (visible.isEmpty()) {
            return List.of();
        }
        return teamRepository.findByIdIn(visible).stream()
                .map(t -> new TeamDTO(t.getId(), t.getName(), t.getCoach()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long id) {
        accessControl.assertCanViewTeam(id);
        return teamRepository.findById(id).orElse(null);
    }

    @Transactional
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

    @Transactional
    public Team updateTeam(Long id, Team team) {
        accessControl.assertCanManageTeam(id);
        team.setId(id);
        Team existing = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));
        team.setOwnerUserId(existing.getOwnerUserId());
        return teamRepository.save(team);
    }

    @Transactional
    public boolean deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            return false;
        }
        accessControl.assertCanManageTeam(id);
        teamRepository.deleteById(id);
        return true;
    }

    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }
}

