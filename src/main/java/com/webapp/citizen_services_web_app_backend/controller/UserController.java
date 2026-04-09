package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // GET all users
    @GetMapping("/users")
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // DELETE user
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // UPDATE user role (using Role enum directly)
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody RoleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String loggedInEmail = "";

        // Extract email from JWT if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.replace("Bearer ", "");
                loggedInEmail = jwtService.extractEmail(token);
            } catch (Exception e) {
                // Invalid token → treat as no logged-in user
            }
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent users from changing their own role
        if (!loggedInEmail.isEmpty() && user.getEmail().equalsIgnoreCase(loggedInEmail)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        // Set the role directly (since it's already a Role enum)
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(UserDTO.fromEntity(updatedUser));
    }

    // CREATE new user (using Role enum directly)
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());                    // Direct Role enum
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true); // default active status

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(UserDTO.fromEntity(savedUser));
    }

    // ====================== Inner Classes (Request & DTO) ======================

    // Request body for updating role
    public static class RoleRequest {
        private Role role;

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }

    // Request body for creating a user
    public static class CreateUserRequest {
        private String email;
        private String password;
        private Role role;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }

    // DTO to avoid exposing password
    public static class UserDTO {
        private Long id;
        private String email;
        private String role;

        public static UserDTO fromEntity(User user) {
            UserDTO dto = new UserDTO();
            dto.id = user.getId();
            dto.email = user.getEmail();
            dto.role = user.getRole() != null ? user.getRole().name() : null;
            return dto;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}