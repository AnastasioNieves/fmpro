package com.tmpro.service;

import com.tmpro.model.Role;
import com.tmpro.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings("null")
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
        return roleRepository.findByNameIgnoreCaseIn(REGISTERABLE_ROLES);
    }

    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
    }
}

