package com.tmpro.security;

import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.repository.RoleRepository;
import com.tmpro.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CurrentUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public SecurityUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debes iniciar sesión.");
        }
        
        // Si el principal ya es un SecurityUser (por ejemplo desde sesión local si la hubiera)
        if (auth.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) auth.getPrincipal();
        }

        // Si viene del FirebaseTokenFilter, el principal es un String uid
        if (auth.getPrincipal() instanceof String) {
            String uid = (String) auth.getPrincipal();
            User user = userRepository.findById(uid).orElseThrow(() -> new AccessDeniedException("Usuario de Firebase no registrado en DB local."));
            Role role = roleRepository.findById(user.getRoleId()).orElseThrow(() -> new AccessDeniedException("Rol no válido."));
            return new SecurityUser(user, role.getName(), user.getTeamId());
        }

        throw new AccessDeniedException("Sesión no válida.");
    }

    public boolean isAdmin(SecurityUser user) {
        return "ADMIN".equalsIgnoreCase(user.getRoleName());
    }

    public boolean isTrainer(SecurityUser user) {
        return "TRAINER".equalsIgnoreCase(user.getRoleName());
    }

    public boolean isUser(SecurityUser user) {
        return "USER".equalsIgnoreCase(user.getRoleName());
    }

    public boolean canManage(SecurityUser user) {
        return isAdmin(user) || isTrainer(user);
    }
}
