package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.Department;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.DepartmentRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000")
public class ComplaintController {

    private final ComplaintRepository complaintRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final JwtService jwtService;

    public ComplaintController(ComplaintRepository complaintRepo,
                               UserRepository userRepo,
                               DepartmentRepository departmentRepo,
                               JwtService jwtService) {
        this.complaintRepo = complaintRepo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitComplaint(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        User citizen = userRepo.findByEmail(email);

        Complaint complaint = new Complaint();
        complaint.setTitle(body.get("title").toString());
        complaint.setDescription(body.getOrDefault("description", "").toString());
        complaint.setCategory(body.get("category").toString().toUpperCase());
        complaint.setArea(body.get("area").toString());
        complaint.setPriority(body.getOrDefault("priority", "MEDIUM").toString().toUpperCase());
        complaint.setCitizen(citizen);

        if (body.containsKey("latitude")) complaint.setLatitude(Double.valueOf(body.get("latitude").toString()));
        if (body.containsKey("longitude")) complaint.setLongitude(Double.valueOf(body.get("longitude").toString()));

        // Auto-assign to department based on category
        departmentRepo.findByName(complaint.getCategory()).ifPresent(complaint::setDepartment);

        complaintRepo.save(complaint);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", complaint.getId());
        resp.put("status", complaint.getStatus());
        resp.put("message", "Complaint submitted successfully");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyComplaints(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        User citizen = userRepo.findByEmail(email);
        List<Complaint> complaints = complaintRepo.findByCitizenId(citizen.getId());
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