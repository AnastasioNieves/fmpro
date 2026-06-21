package com.tmpro.repository;

import com.tmpro.model.Role;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RoleRepository {

    private final Map<String, Role> data = new ConcurrentHashMap<>();

    public Role save(Role role) {
        if (role.getId() == null || role.getId().isEmpty()) {
            role.setId(UUID.randomUUID().toString());
        }
        data.put(role.getId(), role);
        return role;
    }

    public Optional<Role> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Role> findAll() {
        return new ArrayList<>(data.values());
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public Optional<Role> findByName(String name) {
        return data.values().stream()
                .filter(r -> r.getName() != null && r.getName().equals(name))
                .findFirst();
    }
}
