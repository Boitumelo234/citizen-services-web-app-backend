package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintMapDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.DepartmentRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.ComplaintService;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtService jwtService;

    // ==================== Main branch endpoints ====================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submitComplaint(
            @RequestPart("data") @Valid ComplaintRequestDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        ComplaintResponseDTO saved = complaintService.submitComplaint(dto, photo, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Complaint submitted successfully",
                "referenceNumber", saved.getReferenceNumber(),
                "status", saved.getStatus(),
                "data", saved
        ));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaints(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getMyComplaints(authentication));
    }

    @PostMapping(value = "/{id}/updates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> addUpdate(
            @PathVariable("id") Long complaintId,
            @RequestParam("comment") String comment,
            @RequestParam(value = "newLocation", required = false) String newLocation,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        if (comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty"));
        }

        try {
            complaintService.addComplaintUpdate(complaintId, comment, newLocation, photo, authentication);
            return ResponseEntity.ok(Map.of("message", "Update added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add update"));
        }
    }

    @GetMapping("/map")
    public ResponseEntity<List<ComplaintMapDTO>> getComplaintsForMap() {
        List<Complaint> complaints = complaintRepository.findAllWithCoordinates();
        List<ComplaintMapDTO> dtos = complaints.stream()
                .map(c -> new ComplaintMapDTO(
                        c.getId(),
                        c.getReferenceNumber(),
                        c.getCategory(),
                        c.getStatus(),
                        c.getPriority(),
                        c.getLocation(),
                        c.getLatitude(),
                        c.getLongitude(),
                        c.getDescription(),
                        c.getPhotoUrl(),
                        c.getSubmittedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ==================== Legacy branch endpoints (JSON only, no multipart) ====================
    // Mapped to a different path to avoid conflict with the main POST
    @PostMapping("/submit-legacy")
    public ResponseEntity<Map<String, Object>> submitComplaintLegacy(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        User citizen = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Complaint complaint = new Complaint();
        complaint.setTitle(body.get("title").toString());
        complaint.setDescription(body.getOrDefault("description", "").toString());
        complaint.setCategory(body.get("category").toString().toUpperCase());
        complaint.setArea(body.get("area").toString());
        complaint.setPriority(body.getOrDefault("priority", "MEDIUM").toString().toUpperCase());
        complaint.setCitizen(citizen);

        if (body.containsKey("latitude"))
            complaint.setLatitude(Double.valueOf(body.get("latitude").toString()));
        if (body.containsKey("longitude"))
            complaint.setLongitude(Double.valueOf(body.get("longitude").toString()));

        // Auto-assign to department based on category
        departmentRepository.findByName(complaint.getCategory()).ifPresent(complaint::setDepartment);

        complaintRepository.save(complaint);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", complaint.getId());
        resp.put("status", complaint.getStatus());
        resp.put("message", "Complaint submitted successfully");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my-legacy")
    public ResponseEntity<List<Map<String, Object>>> getMyComplaintsLegacy(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        User citizen = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Complaint> complaints = complaintRepository.findByCitizenId(citizen.getId());
        return ResponseEntity.ok(complaints.stream().map(this::toMap).collect(Collectors.toList()));
    }

    private Map<String, Object> toMap(Complaint c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("category", c.getCategory());
        m.put("area", c.getArea());
        m.put("priority", c.getPriority());
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return m;
    }
}