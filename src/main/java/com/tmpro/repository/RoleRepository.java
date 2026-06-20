package com.tmpro.repository;

import com.tmpro.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findByNameIgnoreCaseIn(List<String> names);
}
