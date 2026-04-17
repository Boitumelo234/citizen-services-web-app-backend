package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.ComplaintNote;
import com.webapp.citizen_services_web_app_backend.entity.Notification;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintNoteRepository;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.NotificationRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffController {

    private final ComplaintRepository     complaintRepo;
    private final UserRepository          userRepo;
    private final NotificationRepository  notifRepo;
    private final ComplaintNoteRepository noteRepo;
    private final PasswordEncoder         passwordEncoder;

    private User currentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Staff user not found: " + auth.getName()));
    }

    private void requireOwnership(Complaint c, User staff) {
        boolean owned = c.getAssignedToUser() != null
                && staff.getId().equals(c.getAssignedToUser().getId());
        if (!owned)
            throw new org.springframework.security.access.AccessDeniedException(
                    "Complaint #" + c.getId() + " is not assigned to you.");
    }

    private int priorityScore(String p) {
        if (p == null) return 0;
        return switch (p.toUpperCase()) {
            case "CRITICAL" -> 4; case "HIGH" -> 3; case "MEDIUM" -> 2; case "LOW" -> 1; default -> 0;
        };
    }

    // ── DASHBOARD ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Authentication auth) {
        User staff = currentUser(auth);
        List<Complaint> all = complaintRepo.findByAssignedToId(staff.getId());

        long total      = all.size();
        long pending    = all.stream().filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) || "ASSIGNED".equalsIgnoreCase(c.getStatus())).count();
        long inProgress = all.stream().filter(c -> "IN_PROGRESS".equalsIgnoreCase(c.getStatus())).count();
        long resolved   = all.stream().filter(c -> "RESOLVED".equalsIgnoreCase(c.getStatus())).count();
        long declined   = all.stream().filter(c -> "DECLINED".equalsIgnoreCase(c.getStatus())).count();
        long critical   = all.stream().filter(c -> "CRITICAL".equalsIgnoreCase(c.getPriority())).count();
        double resRate  = total > 0 ? Math.round((resolved * 100.0) / total) : 0;

        OptionalDouble avgDays = all.stream()
                .filter(c -> "RESOLVED".equalsIgnoreCase(c.getStatus()) && c.getCreatedAt() != null && c.getResolvedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toDays())
                .average();

        long resolvedThisWeek = all.stream()
                .filter(c -> "RESOLVED".equalsIgnoreCase(c.getStatus()) && c.getResolvedAt() != null && c.getResolvedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("staffName",         staff.getFullName());
        r.put("departmentName",    staff.getDepartment() != null ? staff.getDepartment().getName() : "N/A");
        r.put("totalAssigned",     total);
        r.put("pending",           pending);
        r.put("inProgress",        inProgress);
        r.put("resolved",          resolved);
        r.put("declined",          declined);
        r.put("critical",          critical);
        r.put("resolutionRate",    resRate);
        r.put("avgResolutionDays", avgDays.isPresent() ? String.format("%.1f", avgDays.getAsDouble()) : "—");
        r.put("resolvedThisWeek",  resolvedThisWeek);
        return ResponseEntity.ok(r);
    }

    // ── COMPLAINTS ───────────────────────────────────────────────────────────

    @GetMapping("/complaints")
    public ResponseEntity<?> getMyComplaints(Authentication auth,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) String category) {

        User staff = currentUser(auth);
        List<Complaint> all = complaintRepo.findByAssignedToId(staff.getId());

        if (status   != null && !status.isEmpty())   all = all.stream().filter(c -> status.equalsIgnoreCase(c.getStatus())).collect(Collectors.toList());
        if (priority != null && !priority.isEmpty()) all = all.stream().filter(c -> priority.equalsIgnoreCase(c.getPriority())).collect(Collectors.toList());
        if (category != null && !category.isEmpty()) all = all.stream().filter(c -> category.equalsIgnoreCase(c.getCategory())).collect(Collectors.toList());

        all.sort((a, b) -> {
            int d = priorityScore(b.getPriority()) - priorityScore(a.getPriority());
            if (d != 0) return d;
            if (b.getCreatedAt() != null && a.getCreatedAt() != null) return b.getCreatedAt().compareTo(a.getCreatedAt());
            return 0;
        });

        return ResponseEntity.ok(all.stream().map(this::toComplaintMap).collect(Collectors.toList()));
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<?> getComplaint(@PathVariable Long id, Authentication auth) {
        User staff = currentUser(auth);
        Complaint c = complaintRepo.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        requireOwnership(c, staff);

        Map<String, Object> dto = toComplaintMap(c);
        List<Map<String, Object>> notes = noteRepo.findByComplaintIdOrderByCreatedAtAsc(id).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId()); m.put("authorName", n.getAuthorName());
            m.put("note", n.getNote()); m.put("createdAt", n.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        dto.put("notes", notes);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        User staff = currentUser(auth);
        Complaint c = complaintRepo.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        requireOwnership(c, staff);

        String upper = (body.get("status") + "").toUpperCase();
        if (!Set.of("PENDING","ASSIGNED","IN_PROGRESS","RESOLVED","DECLINED").contains(upper))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + upper));

        c.setStatus(upper);
        if ("RESOLVED".equals(upper) && c.getResolvedAt() == null) c.setResolvedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        complaintRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "Status updated", "status", upper));
    }

    @PostMapping("/complaints/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        User staff = currentUser(auth);
        Complaint c = complaintRepo.findById(id).orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        requireOwnership(c, staff);

        String text = body.get("note");
        if (text == null || text.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Note cannot be empty"));

        ComplaintNote note = new ComplaintNote();
        note.setComplaint(c);
        note.setAuthorId(staff.getId());
        note.setAuthorName(staff.getFullName() != null ? staff.getFullName() : staff.getEmail());
        note.setNote(text.trim());
        note.setCreatedAt(LocalDateTime.now());
        noteRepo.save(note);

        c.setUpdatedAt(LocalDateTime.now());
        complaintRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "Note added"));
    }

    // ── NOTIFICATIONS ────────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Authentication auth) {
        User staff = currentUser(auth);
        return ResponseEntity.ok(notifRepo.findByUserOrderByCreatedAtDesc(staff).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId()); m.put("type", n.getType() != null ? n.getType() : "SYSTEM");
            m.put("message", n.getMessage()); m.put("read", n.isRead()); m.put("createdAt", n.getCreatedAt());
            return m;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        User staff = currentUser(auth);
        notifRepo.findByIdAndUser(id, staff).ifPresent(n -> { n.setRead(true); notifRepo.save(n); });
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        User staff = currentUser(auth);
        List<Notification> list = notifRepo.findByUserOrderByCreatedAtDesc(staff);
        list.forEach(n -> n.setRead(true));
        notifRepo.saveAll(list);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    // ── PROFILE ──────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        return ResponseEntity.ok(toProfileMap(currentUser(auth)));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        User staff = currentUser(auth);
        if (body.containsKey("fullName")) staff.setFullName(body.get("fullName"));
        if (body.containsKey("phone"))    staff.setPhone(body.get("phone"));
        userRepo.save(staff);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Authentication auth) {
        User staff = currentUser(auth);
        String currentPw = body.get("currentPassword");
        String newPw     = body.get("newPassword");
        if (!passwordEncoder.matches(currentPw, staff.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        if (newPw == null || newPw.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        staff.setPassword(passwordEncoder.encode(newPw));
        userRepo.save(staff);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    // ── MAPPERS ──────────────────────────────────────────────────────────────

    private Map<String, Object> toComplaintMap(Complaint c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          c.getId());
        m.put("title",       c.getTitle());
        m.put("description", c.getDescription());
        m.put("category",    c.getCategory());
        m.put("priority",    c.getPriority());
        m.put("status",      c.getStatus());
        m.put("area",        c.getArea() != null ? c.getArea() : c.getLocation());
        m.put("photoUrl",    c.getPhotoUrl());
        m.put("createdAt",   c.getCreatedAt());
        m.put("updatedAt",   c.getUpdatedAt());
        m.put("resolvedAt",  c.getResolvedAt());
        User citizen = c.getUser() != null ? c.getUser() : c.getCitizen();
        if (citizen != null) { m.put("citizenEmail", citizen.getEmail()); m.put("citizenName", citizen.getFullName()); }
        if (c.getDepartment() != null) m.put("departmentName", c.getDepartment().getName());
        if (c.getAssignedToUser() != null) { m.put("assignedToEmail", c.getAssignedToUser().getEmail()); m.put("assignedToName", c.getAssignedToUser().getFullName()); }
        return m;
    }

    private Map<String, Object> toProfileMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             u.getId());
        m.put("email",          u.getEmail());
        m.put("fullName",       u.getFullName());
        m.put("phone",          u.getPhone());
        m.put("role",           u.getRole() != null ? u.getRole() : "STAFF");
        m.put("departmentName", u.getDepartment() != null ? u.getDepartment().getName() : null);
        m.put("active",         Boolean.TRUE.equals(u.getActive()));
        return m;
    }
}