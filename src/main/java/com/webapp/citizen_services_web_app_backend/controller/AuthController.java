package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;           // Can be null if not configured
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    // Constructor with Optional to prevent startup failure when mail is not configured
    public AuthController(
            UserRepository userRepository,
            JwtService jwtService,
            Optional<JavaMailSender> mailSenderOptional) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.mailSender = mailSenderOptional.orElse(null);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null || email.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email and password are required"));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        Role role = email.equalsIgnoreCase(adminEmail) ? Role.ADMIN : Role.CITIZEN;

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);                    // Good practice

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String username,
            @RequestParam String password) {

        Optional<User> optionalUser = userRepository.findByEmail(username);

        if (optionalUser.isEmpty() ||
                !passwordEncoder.matches(password, optionalUser.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        User user = optionalUser.get();

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "message", "Login successful"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            // Security best practice: don't reveal if email exists
            return ResponseEntity.ok(Map.of("message", "If an account exists, a reset code will be sent."));
        }

        User user = optionalUser.get();
        String token = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Log the token for now (you can remove this later)
        System.out.println("=== PASSWORD RESET CODE ===");
        System.out.println("Email : " + email);
        System.out.println("Code  : " + token);
        System.out.println("Expires in 15 minutes");
        System.out.println("===========================");

        // TODO: When you're ready, send real email:
        // if (mailSender != null) {
        //     sendResetCodeEmail(email, token);
        // }

        return ResponseEntity.ok(Map.of(
                "message", "If an account exists, a reset code has been sent to your email.",
                "token", token          // ← Remove this in production for security!
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token and new password are required"));
        }

        User user = userRepository.findByResetToken(token);

        if (user == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "The reset token is invalid or has expired."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }
}