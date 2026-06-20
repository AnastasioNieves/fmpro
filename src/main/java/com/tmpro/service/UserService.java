package com.tmpro.service;

import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.model.UserRequest;
import com.tmpro.model.UserResponse;
import com.tmpro.repository.TeamRepository;
import com.tmpro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@SuppressWarnings("null")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TeamRepository teamRepository;

    public UserResponse registerUser(UserRequest userRequest) {
        if (userRepository.existsByUsernameIgnoreCase(userRequest.getUsername().trim())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        if ("admin".equalsIgnoreCase(userRequest.getUsername().trim())) {
            throw new IllegalArgumentException("Este nombre de usuario está reservado.");
        }

        Long requestedRoleId = userRequest.getRoleId() != null ? userRequest.getRoleId() : userRequest.getRole();
        final Long roleId = requestedRoleId != null ? requestedRoleId : 2L;

        Role role = roleService.getById(roleId);

        if ("ADMIN".equalsIgnoreCase(role.getName())) {
            throw new IllegalArgumentException("No puedes registrarte como administrador.");
        }

        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setRoleId(role.getId());

        if ("USER".equalsIgnoreCase(role.getName())) {
            if (userRequest.getTeamId() == null) {
                throw new IllegalArgumentException("Selecciona un equipo para seguir como usuario.");
            }
            if (!teamRepository.existsById(userRequest.getTeamId())) {
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

