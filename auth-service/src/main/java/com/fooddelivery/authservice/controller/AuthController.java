package com.fooddelivery.authservice.controller;

import com.fooddelivery.authservice.authservice.AuthService;
import com.fooddelivery.authservice.dto.LoginRequest;
import com.fooddelivery.authservice.dto.LoginResponse;
import com.fooddelivery.authservice.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Only an existing ROLE_ADMIN can promote another user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/promote/{username}")
    public ResponseEntity<Void> promote(@PathVariable String username) {
        authService.promoteToAdmin(username);
        return ResponseEntity.ok().build();
    }
}