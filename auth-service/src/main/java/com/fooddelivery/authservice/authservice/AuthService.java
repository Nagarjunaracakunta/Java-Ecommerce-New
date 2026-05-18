package com.fooddelivery.authservice.authservice;

import com.fooddelivery.authservice.dto.LoginRequest;
import com.fooddelivery.authservice.dto.LoginResponse;
import com.fooddelivery.authservice.dto.RegisterRequest;
import com.fooddelivery.authservice.entity.Role;
import com.fooddelivery.authservice.entity.UserEntity;
import com.fooddelivery.authservice.repository.UserRepository;
import com.fooddelivery.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return LoginResponse.of(token, expirationMs);
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserEntity user = new UserEntity(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER  // server always assigns default role — never trust frontend
        );
        userRepository.save(user);
    }

    public void promoteToAdmin(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
    }
}