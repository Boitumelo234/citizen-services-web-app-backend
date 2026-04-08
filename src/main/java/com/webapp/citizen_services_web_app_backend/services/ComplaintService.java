package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    // ─── Submit complaint ─────────────────────────────────────────────────────
    public Complaint submitComplaint(String title, String category, String description,
                                     String area, String priority, String citizenEmail) {
        User citizen = userRepository.findByEmail(citizenEmail);
        if (citizen == null) {
            throw new RuntimeException("Citizen not found with email: " + citizenEmail);
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(title);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setArea(area);
        complaint.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");
        complaint.setStatus("PENDING");
        complaint.setCitizen(citizen);

        return complaintRepository.save(complaint);
    }

    // ─── Get all complaints ───────────────────────────────────────────────────
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // ─── Get complaint by id ──────────────────────────────────────────────────
    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    // ─── Update status ────────────────────────────────────────────────────────
    public Complaint updateStatus(Long id, String newStatus, String assignedToEmail) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        complaint.setStatus(newStatus.toUpperCase());

        if (assignedToEmail != null && !assignedToEmail.isBlank()) {
            User staff = userRepository.findByEmail(assignedToEmail);
            if (staff != null) {
                complaint.setAssignedTo(staff);
            }
        }

        return complaintRepository.save(complaint);
    }

    // ─── Dashboard stats ──────────────────────────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total      = complaintRepository.count();
        long resolved   = complaintRepository.countByStatus("RESOLVED");
        long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
        long pending    = complaintRepository.countByStatus("PENDING");
        long assigned   = complaintRepository.countByStatus("ASSIGNED");

        stats.put("totalComplaints",    total);
        stats.put("resolvedComplaints", resolved);
        stats.put("inProgress",         inProgress);
        stats.put("pendingComplaints",  pending);
        stats.put("assignedComplaints", assigned);

        // ── Status distribution ───────────────────────────────────────────────
        List<Object[]> statusData = complaintRepository.countByStatusGrouped();
        List<Map<String, Object>> statusDist = statusData.stream().map(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", row[0]);
            entry.put("count",  row[1]);
            return entry;
        }).collect(Collectors.toList());
        stats.put("statusDistribution", statusDist);

        // ── Recent complaints ─────────────────────────────────────────────────
        List<Complaint> recent = complaintRepository.findAll().stream()
                .sorted(Comparator.comparing(Complaint::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> recentList = recent.stream().map(c -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",        c.getId());
            entry.put("title",     c.getTitle());
            entry.put("category",  c.getCategory());
            entry.put("area",      c.getArea());
            entry.put("status",    c.getStatus());
            entry.put("priority",  c.getPriority());
            entry.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            return entry;
        }).collect(Collectors.toList());
        stats.put("recentComplaints", recentList);

        return stats;
    }
}