package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${admin.email}")
    private String adminEmail;

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // ── Register ──────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody Map<String, String> request) {

        String email    = request.get("email");
        String password = request.get("password");
        String fullName = request.getOrDefault("fullName", "");
        String phone    = request.getOrDefault("phone", "");

        if (email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email and password are required"));
        }

        // findByEmail returns User directly (not Optional) — use != null
        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        // Admin email → ADMIN role, everyone else → CITIZEN
        String role = email.equalsIgnoreCase(adminEmail) ? "ADMIN" : "CITIZEN";

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setActive(true);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password) {

        // findByEmail returns User directly — plain null check, no Optional
        User user = userRepository.findByEmail(username);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        // getActive() now works because Boolean (wrapper) generates getActive()
        // primitive boolean would generate isActive() — that's the bug we fixed
        if (!Boolean.TRUE.equals(user.getActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error",
                            "Account deactivated. Contact the administrator."));
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type",   "Bearer",
                "role",         user.getRole(),
                "message",      "Login successful"
        ));
    }
}