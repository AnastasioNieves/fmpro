package com.tmpro.service;

import com.tmpro.model.AuthResponse;
import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.repository.UserRepository;
import com.tmpro.security.SecurityUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

@Service
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            RoleService roleService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(String username, String password, HttpServletRequest request) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username.trim(), password));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return buildAuthResponse((SecurityUser) authentication.getPrincipal());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public Optional<AuthResponse> resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            return Optional.empty();
        }
        return Optional.of(buildAuthResponse(user));
    }

    private AuthResponse buildAuthResponse(SecurityUser user) {
        User entity = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Role role = roleService.getById(entity.getRoleId());
        return new AuthResponse(
                entity.getId(),
                entity.getUsername(),
                role.getId(),
                role.getName(),
                entity.getTeamId()
        );
    }
}

