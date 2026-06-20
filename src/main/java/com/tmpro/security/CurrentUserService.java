package com.tmpro.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public SecurityUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debes iniciar sesión.");
        }
        if (!(auth.getPrincipal() instanceof SecurityUser user)) {
            throw new AccessDeniedException("Sesión no válida.");
        }
        return user;
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
