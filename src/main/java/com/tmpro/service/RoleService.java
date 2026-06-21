package com.tmpro.service;

import com.tmpro.model.Role;
import com.tmpro.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class RoleService {

    private static final List<String> REGISTERABLE_ROLES = List.of("USER", "TRAINER");

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public List<Role> findRegisterable() {
        return roleRepository.findAll().stream()
            .filter(r -> REGISTERABLE_ROLES.contains(r.getName().toUpperCase()))
            .collect(Collectors.toList());
    }

    public Role getById(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }
}
