package com.tmpro.controller;

import com.tmpro.model.AuthResponse;
import com.tmpro.model.User;
import com.tmpro.model.UserRequest;
import com.tmpro.model.UserResponse;
import com.tmpro.service.AuthService;
import com.tmpro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @RequestBody UserRequest userRequest,
            HttpServletRequest request
    ) {
        UserResponse userResponse = userService.registerUser(userRequest);
        authService.login(userRequest.getUsername(), userRequest.getPassword(), request);
        return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody User user, HttpServletRequest request) {
        AuthResponse response = authService.login(user.getUsername(), user.getPassword(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return authService.resolveCurrentUser()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
