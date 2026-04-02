package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email and password are required")
            );
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email already registered")
            );
        }

        Role role = email.equalsIgnoreCase(adminEmail)
                ? Role.ADMIN
                : Role.CITIZEN;

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        //user.setRole(Role.valueOf(String.valueOf(role)));
        user.setRole(role);

        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "Registration successful")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password) {

        Optional<User> optionalUser = userRepository.findByEmail(username);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid email or password")
            );
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid email or password")
            );
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()   // convert enum to String
        );

        return ResponseEntity.ok(
                Map.of(
                        "access_token", token,
                        "token_type", "Bearer",
                        "message", "Login successful"
                )
        );
    }
}
