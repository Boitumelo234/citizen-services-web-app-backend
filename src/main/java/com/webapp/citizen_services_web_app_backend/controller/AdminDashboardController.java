package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.Department;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.DepartmentRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminDashboardController {

    private final ComplaintRepository complaintRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final ComplaintService complaintService;

    public AdminDashboardController(ComplaintRepository complaintRepo,
                                    UserRepository userRepo,
                                    DepartmentRepository departmentRepo,
                                    ComplaintService complaintService) {
        this.complaintRepo = complaintRepo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.complaintService = complaintService;
    }

    // ==================== Your Branch Version (Primary) ====================

    @GetMapping("/overview/stats")
    public ResponseEntity<Map<String, Object>> getOverviewStats() {
        long total = complaintRepo.count();
        long pending = complaintRepo.countByStatus("PENDING");
        long inProgress = complaintRepo.countByStatus("IN_PROGRESS") + complaintRepo.countByStatus("ASSIGNED");
        long resolvedToday = complaintRepo.findResolvedSince(LocalDateTime.now().toLocalDate().atStartOfDay()).size();
        long totalUsers = userRepo.countByRole("CITIZEN");
        long activeStaff = userRepo.findByRole("STAFF").stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive())).count();
        long unassigned = complaintRepo.findByAssignedToIsNull().size();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalComplaints", total);
        stats.put("pending", pending);
        stats.put("inProgress", inProgress);
        stats.put("resolvedToday", resolvedToday);
        stats.put("totalCitizens", totalUsers);
        stats.put("activeStaff", activeStaff);
        stats.put("unassigned", unassigned);
        stats.put("criticalPending", complaintRepo.countByStatus("PENDING"));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/overview/charts/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyComplaints() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = complaintRepo.dailyComplaintCounts(since);
        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0].toString());
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview/charts/by-category")
    public ResponseEntity<List<Map<String, Object>>> getByCategory() {
        List<Object[]> rows = complaintRepo.countByCategory();
        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview/charts/by-status")
    public ResponseEntity<List<Map<String, Object>>> getByStatus() {
        List<Object[]> rows = complaintRepo.countByStatusGrouped();
        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview/department-activity")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentActivity() {
        List<Department> departments = departmentRepo.findAll();
        List<Map<String, Object>> result = departments.stream().map(dept -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", dept.getId());
            m.put("name", dept.getName());
            m.put("activeComplaints", complaintRepo.findByDepartmentId(dept.getId())
                    .stream().filter(c -> !"RESOLVED".equals(c.getStatus())).count());
            m.put("staffCount", userRepo.findByRoleAndDepartmentId("STAFF", dept.getId()).size());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts() {
        List<Complaint> unassigned = complaintRepo.findByAssignedToIsNull();
        List<Complaint> highPriority = complaintRepo.findByPriority("CRITICAL");
        List<Complaint> overdue = complaintRepo.findOverdueComplaints(LocalDateTime.now().minusDays(3));

        Map<String, Object> alerts = new LinkedHashMap<>();
        alerts.put("unassignedCount", unassigned.size());
        alerts.put("criticalCount", highPriority.stream().filter(c -> !"RESOLVED".equals(c.getStatus())).count());
        alerts.put("overdueCount", overdue.size());

        List<Map<String, Object>> alertList = new ArrayList<>();
        highPriority.stream().filter(c -> !"RESOLVED".equals(c.getStatus())).limit(5).forEach(c -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", c.getId());
            a.put("type", "CRITICAL");
            a.put("message", "Critical complaint: " + c.getTitle() + " in " + c.getArea());
            a.put("complaintId", c.getId());
            alertList.add(a);
        });
        overdue.stream().limit(5).forEach(c -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "overdue-" + c.getId());
            a.put("type", "OVERDUE");
            a.put("message", "Overdue complaint #" + c.getId() + ": " + c.getTitle());
            a.put("complaintId", c.getId());
            alertList.add(a);
        });
        unassigned.stream().limit(3).forEach(c -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "unassigned-" + c.getId());
            a.put("type", "UNASSIGNED");
            a.put("message", "Unassigned: " + c.getTitle() + " (" + c.getCategory() + ")");
            a.put("complaintId", c.getId());
            alertList.add(a);
        });
        alerts.put("alerts", alertList);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/overview/map-incidents")
    public ResponseEntity<List<Map<String, Object>>> getMapIncidents() {
        List<Complaint> complaints = complaintRepo.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .collect(Collectors.toList());
        List<Map<String, Object>> result = complaints.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("category", c.getCategory());
            m.put("status", c.getStatus());
            m.put("priority", c.getPriority());
            m.put("lat", c.getLatitude());
            m.put("lng", c.getLongitude());
            m.put("area", c.getArea());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<Map<String, Object>>> getAllComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category) {

        List<Complaint> complaints;
        if (status != null) {
            complaints = complaintRepo.findByStatus(status.toUpperCase());
        } else if (priority != null) {
            complaints = complaintRepo.findByPriority(priority.toUpperCase());
        } else if (category != null) {
            complaints = complaintRepo.findByCategory(category.toUpperCase());
        } else {
            complaints = complaintRepo.findAll();
        }

        return ResponseEntity.ok(complaints.stream()
                .map(this::toComplaintMap)
                .collect(Collectors.toList()));
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<Map<String, Object>> getComplaint(@PathVariable Long id) {
        return complaintRepo.findById(id)
                .map(c -> ResponseEntity.ok(toComplaintMap(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/complaints/{id}/assign")
    public ResponseEntity<Map<String, Object>> assignComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        return complaintRepo.findById(id).map(complaint -> {
            Long staffId = Long.valueOf(body.get("staffId").toString());
            User staff = userRepo.findById(staffId).orElseThrow(() -> new RuntimeException("Staff not found"));

            complaint.setAssignedToUser(staff);           // Important: set the relationship
            complaint.setAssignedTo(staff.getEmail());    // Keep string fallback
            complaint.setStatus("ASSIGNED");
            if (staff.getDepartment() != null) {
                complaint.setDepartment(staff.getDepartment());
            }

            complaintRepo.save(complaint);
            return ResponseEntity.ok(toComplaintMap(complaint));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return complaintRepo.findById(id).map(complaint -> {
            complaint.setStatus(body.get("status").toUpperCase());
            complaintRepo.save(complaint);
            return ResponseEntity.ok(toComplaintMap(complaint));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/complaints/{id}/escalate")
    public ResponseEntity<Map<String, Object>> escalateComplaint(@PathVariable Long id) {
        return complaintRepo.findById(id).map(complaint -> {
            complaint.setPriority("CRITICAL");
            complaintRepo.save(complaint);
            return ResponseEntity.ok(toComplaintMap(complaint));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(@RequestParam(required = false) String role) {
        List<User> users = role != null ? userRepo.findByRole(role.toUpperCase()) : userRepo.findAll();
        return ResponseEntity.ok(users.stream().map(this::toUserMap).collect(Collectors.toList()));
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setEmail(body.get("email").toString());
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode(body.get("password").toString()));
        user.setRole(body.get("role").toString().toUpperCase());
        user.setFullName(body.getOrDefault("fullName", "").toString());
        user.setPhone(body.getOrDefault("phone", "").toString());
        user.setActive(Boolean.TRUE);
        if (body.containsKey("departmentId") && body.get("departmentId") != null) {
            Long deptId = Long.valueOf(body.get("departmentId").toString());
            departmentRepo.findById(deptId).ifPresent(user::setDepartment);
        }
        userRepo.save(user);
        return ResponseEntity.ok(toUserMap(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(user -> {
            if (body.containsKey("fullName")) user.setFullName(body.get("fullName").toString());
            if (body.containsKey("phone")) user.setPhone(body.get("phone").toString());
            if (body.containsKey("role")) user.setRole(body.get("role").toString().toUpperCase());
            if (body.containsKey("active")) user.setActive(Boolean.parseBoolean(body.get("active").toString()));
            if (body.containsKey("departmentId") && body.get("departmentId") != null) {
                Long deptId = Long.valueOf(body.get("departmentId").toString());
                departmentRepo.findById(deptId).ifPresent(user::setDepartment);
            }
            userRepo.save(user);
            return ResponseEntity.ok(toUserMap(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartments() {
        return ResponseEntity.ok(departmentRepo.findAll().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("description", d.getDescription());
            m.put("staffCount", userRepo.findByRoleAndDepartmentId("STAFF", d.getId()).size());
            m.put("activeComplaints", complaintRepo.findByDepartmentId(d.getId())
                    .stream().filter(c -> !"RESOLVED".equals(c.getStatus())).count());
            return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/departments")
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody Map<String, String> body) {
        Department dept = new Department();
        dept.setName(body.get("name").toUpperCase());
        dept.setDescription(body.getOrDefault("description", ""));
        departmentRepo.save(dept);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", dept.getId());
        result.put("name", dept.getName());
        result.put("description", dept.getDescription());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>> getReportSummary(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Complaint> recent = complaintRepo.findSince(since);
        List<Complaint> resolved = complaintRepo.findResolvedSince(since);

        Map<String, Long> byCategory = recent.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));
        Map<String, Long> byStatus = recent.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
        Map<String, Long> byArea = recent.stream()
                .collect(Collectors.groupingBy(Complaint::getArea, Collectors.counting()));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", days + " days");
        report.put("totalComplaints", recent.size());
        report.put("resolved", resolved.size());
        report.put("resolutionRate", recent.size() > 0 ? (resolved.size() * 100.0 / recent.size()) : 0);
        report.put("byCategory", byCategory);
        report.put("byStatus", byStatus);
        report.put("hotspots", byArea);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/staff-performance")
    public ResponseEntity<List<Map<String, Object>>> getStaffPerformance() {
        List<User> staff = userRepo.findByRole("STAFF");
        List<Map<String, Object>> result = staff.stream().map(s -> {
            List<Complaint> assigned = complaintRepo.findByAssignedToId(s.getId());
            long resolved = assigned.stream().filter(c -> "RESOLVED".equals(c.getStatus())).count();
            long declined = assigned.stream().filter(c -> "DECLINED".equals(c.getStatus())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("staffId", s.getId());
            m.put("name", s.getFullName() != null ? s.getFullName() : s.getEmail());
            m.put("department", s.getDepartment() != null ? s.getDepartment().getName() : "N/A");
            m.put("assigned", assigned.size());
            m.put("resolved", resolved);
            m.put("declined", declined);
            m.put("resolutionRate", assigned.size() > 0 ? (resolved * 100.0 / assigned.size()) : 0);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ==================== Unique Methods from Main Branch (No Duplicates) ====================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    @GetMapping("/users/count")
    public ResponseEntity<Map<String, Long>> getUserCount() {
        long total = userRepo.count();
        long admins = userRepo.countByRole("ADMIN");
        long citizens = userRepo.countByRole("CITIZEN");
        return ResponseEntity.ok(Map.of(
                "total", total,
                "admins", admins,
                "citizens", citizens
        ));
    }

    @PostMapping("/complaints")
    public ResponseEntity<?> createComplaint(@RequestBody Map<String, String> body) {
        try {
            // This is a placeholder – adjust according to your actual creation logic
            Complaint created = new Complaint();
            // Set fields from body if needed
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("active", Boolean.TRUE.equals(u.getActive()));

        if (u.getDepartment() != null) {
            m.put("departmentId", u.getDepartment().getId());
            m.put("departmentName", u.getDepartment().getName());
        }
        return m;
    }

    private Map<String, Object> toComplaintMap(Complaint c) {
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("description", c.getDescription());
        m.put("category", c.getCategory());
        m.put("area", c.getArea());
        m.put("priority", c.getPriority());
        m.put("status", c.getStatus());
        m.put("latitude", c.getLatitude());
        m.put("longitude", c.getLongitude());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("resolvedAt", c.getResolvedAt() != null ? c.getResolvedAt().toString() : null);
        m.put("photoUrl", c.getPhotoUrl());                    // ← Critical for thumbnail

        // Citizen info
        if (c.getUser() != null) {
            m.put("citizenId", c.getUser().getId());
            m.put("citizenEmail", c.getUser().getEmail());
            m.put("citizenName", c.getUser().getFullName());
        } else if (c.getCitizen() != null) {
            m.put("citizenId", c.getCitizen().getId());
            m.put("citizenEmail", c.getCitizen().getEmail());
            m.put("citizenName", c.getCitizen().getFullName());
        }

        // ASSIGNED STAFF - This is the key fix
        if (c.getAssignedToUser() != null) {
            m.put("assignedToId", c.getAssignedToUser().getId());
            m.put("assignedToEmail", c.getAssignedToUser().getEmail());
            m.put("assignedToName", c.getAssignedToUser().getFullName());
        } else if (c.getAssignedTo() != null) {
            m.put("assignedToEmail", c.getAssignedTo().toString());
            m.put("assignedTo", c.getAssignedTo().toString());
        }

        // Department
        if (c.getDepartment() != null) {
            m.put("departmentId", c.getDepartment().getId());
            m.put("departmentName", c.getDepartment().getName());
        }

        return m;
    }
}