package com.tmpro.service;

import com.tmpro.model.AuthResponse;
import com.tmpro.model.Role;
import com.tmpro.model.User;
import com.tmpro.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleService roleService;

    public AuthService(
            UserRepository userRepository,
            RoleService roleService
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    public AuthResponse login(String username, String password, HttpServletRequest request) {
        // En Firebase Auth, el login real se hace en el frontend. 
        // Si se llama a este endpoint, simplemente podemos resolver el usuario si el token vino en el header
        // o lanzar una excepción de que debe loguearse por Firebase.
        return resolveCurrentUser().orElseThrow(() -> new IllegalArgumentException("El usuario debe loguearse usando Firebase en el cliente."));
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
    }

    public Optional<AuthResponse> resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        
        // El FirebaseTokenFilter pone el UID de Firebase como el principal de la autenticación
        String uid = (String) authentication.getPrincipal();
        return Optional.of(buildAuthResponse(uid));
    }

    private AuthResponse buildAuthResponse(String uid) {
        User entity = userRepository.findById(uid)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(uid);
                    newUser.setUsername("User-" + uid.substring(0, Math.min(uid.length(), 5)));
                    Role userRole = roleService.findByName("USER")
                            .orElseThrow(() -> new IllegalStateException("Role USER not found"));
                    newUser.setRoleId(userRole.getId());
                    return userRepository.save(newUser);
                });
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
