package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminDashboardController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    public AdminDashboardController(ComplaintService complaintService,
                                    UserRepository userRepository) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
    }

    // ─── GET /api/admin/dashboard ─────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    // ─── GET /api/admin/complaints ────────────────────────────────────────────
    @GetMapping("/complaints")
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // ─── GET /api/admin/complaints/{id} ──────────────────────────────────────
    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaintById(@PathVariable Long id) {
        Optional<Complaint> complaint = complaintService.getComplaintById(id);
        return complaint.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    // ─── PUT /api/admin/complaints/{id}/status ────────────────────────────────
    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String status     = body.get("status");
            String assignedTo = body.get("assignedTo");
            Complaint updated = complaintService.updateStatus(id, status, assignedTo);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── GET /api/admin/users ─────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // ─── GET /api/admin/users/count ───────────────────────────────────────────
    @GetMapping("/users/count")
    public ResponseEntity<Map<String, Long>> getUserCount() {
        long total    = userRepository.count();
        // Use the Role enum and the built-in countByRole from UserRepository
        long admins   = userRepository.countByRole(Role.ADMIN);
        long citizens = userRepository.countByRole(Role.CITIZEN);
        return ResponseEntity.ok(Map.of(
                "total",    total,
                "admins",   admins,
                "citizens", citizens
        ));
    }

    // ─── POST /api/admin/complaints ───────────────────────────────────────────
    @PostMapping("/complaints")
    public ResponseEntity<?> createComplaint(@RequestBody Map<String, String> body) {
        try {
            Complaint created = complaintService.submitComplaint(
                    body.get("category"),
                    body.get("description"),
                    body.get("area"),
                    body.get("priority"),
                    body.get("citizenEmail")
            );
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}