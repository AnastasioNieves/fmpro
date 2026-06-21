package com.tmpro.service;

import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.model.UserRequest;
import com.tmpro.model.UserResponse;
import com.tmpro.repository.TeamRepository;
import com.tmpro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@SuppressWarnings("all")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TeamRepository teamRepository;

    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByUsernameIgnoreCase(userRequest.getUsername().trim())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        if ("admin".equalsIgnoreCase(userRequest.getUsername().trim())) {
            throw new IllegalArgumentException("Este nombre de usuario está reservado.");
        }

        String requestedRoleId = userRequest.getRoleId() != null ? userRequest.getRoleId() : userRequest.getRole();
        // Firebase Auth normally handles passwords, but for local reference we might hash it.
        // If "2L" was used as role ID before, now it's a string, e.g. "USER_ROLE_ID". We rely on RoleService.
        
        // Find role by Name rather than default ID to be safe if ID changed
        Role role;
        if (requestedRoleId == null || requestedRoleId.isEmpty()) {
            role = roleService.findByName("USER").orElseThrow(() -> new IllegalStateException("Rol USER no encontrado en DB"));
        } else {
            role = roleService.getById(requestedRoleId);
        }

        if ("ADMIN".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("No puedes registrarte como administrador.");
        }

        if (userRequest.getId() == null || userRequest.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Se requiere el ID de Firebase para completar el registro.");
        }

        User user = new User();
        user.setId(userRequest.getId());
        user.setUsername(userRequest.getUsername());
        user.setPassword(""); // Firebase Auth gestiona las contraseñas
        user.setRoleId(role.getId());

        if ("USER".equalsIgnoreCase(role.getName())) {
            if (userRequest.getTeamId() == null) {
                throw new IllegalArgumentException("Selecciona un equipo para seguir como usuario.");
            }
            if (teamRepository.findById(userRequest.getTeamId()).isEmpty()) {
                throw new IllegalArgumentException("El equipo seleccionado no existe.");
            }
            user.setTeamId(userRequest.getTeamId());
        }

        userRepository.save(user);

        return new UserResponse(user.getId(), user.getUsername(), user.getRoleId(), role.getName());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
