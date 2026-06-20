package com.tmpro.security;

import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.repository.UserRepository;
import com.tmpro.service.RoleService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FmproUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    public FmproUserDetailsService(UserRepository userRepository, RoleService roleService) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        Role role = roleService.getById(user.getRoleId());
        return new SecurityUser(user, role.getName(), user.getTeamId());
    }
}
