package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    // ─── Generate reference number ────────────────────────────────────────────
    public String generateReferenceNumber() {
        long count = complaintRepository.count() + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("RLM-%d-%03d", year, count);
    }

    // ─── Submit complaint ─────────────────────────────────────────────────────
    public Complaint submitComplaint(String category, String description,
                                     String area, String priority,
                                     String citizenEmail) {
        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Citizen not found with email: " + citizenEmail));

        Complaint complaint = new Complaint();
        complaint.setReferenceNumber(generateReferenceNumber());
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setArea(area);
        complaint.setPriority(priority != null ? priority : "Medium");
        complaint.setStatus("New");
        complaint.setCitizen(citizen);
        return complaintRepository.save(complaint);
    }

    // ─── Get all complaints ───────────────────────────────────────────────────
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    // ─── Get complaint by id ──────────────────────────────────────────────────
    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    // ─── Update status ────────────────────────────────────────────────────────
    public Complaint updateStatus(Long id, String newStatus, String assignedTo) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Complaint not found with id: " + id));

        complaint.setStatus(newStatus);
        if (assignedTo != null && !assignedTo.isBlank()) {
            complaint.setAssignedTo(assignedTo);
        }
        if ("Resolved".equalsIgnoreCase(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        return complaintRepository.save(complaint);
    }

    // ─── Dashboard stats ──────────────────────────────────────────────────────
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total      = complaintRepository.count();
        long resolved   = complaintRepository.countByStatus("Resolved");
        long inProgress = complaintRepository.countByStatus("In Progress");
        long newCount   = complaintRepository.countByStatus("New");
        long open       = newCount + inProgress;

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime weekStart    = LocalDateTime.now().minusDays(7);

        long newThisMonth      = complaintRepository.countNewThisMonth(startOfMonth);
        long resolvedThisMonth = complaintRepository.countResolvedThisMonth(startOfMonth);
        long newThisWeek       = complaintRepository.countComplaintsThisWeek(weekStart);

        Double avgHours = complaintRepository.getAvgResolutionTimeHours();
        double avgDays  = (avgHours != null)
                ? Math.round((avgHours / 24.0) * 10.0) / 10.0
                : 0.0;

        stats.put("totalComplaints",    total);
        stats.put("openComplaints",     open);
        stats.put("resolvedComplaints", resolved);
        stats.put("pendingComplaints",  newCount);
        stats.put("newThisMonth",       newThisMonth);
        stats.put("resolvedThisMonth",  resolvedThisMonth);
        stats.put("avgResolutionDays",  avgDays);
        stats.put("newThisWeek",        newThisWeek);

        // ── Status distribution ───────────────────────────────────────────────
        List<Object[]> statusData = complaintRepository.getStatusCounts();
        List<Map<String, Object>> statusDist = new ArrayList<>();
        for (Object[] row : statusData) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", row[0]);
            entry.put("count",  row[1]);
            statusDist.add(entry);
        }
        stats.put("statusDistribution", statusDist);

        // ── Department performance ────────────────────────────────────────────
        // Now uses DepartmentPerformanceResponse directly — no more Object[]
        List<DepartmentPerformanceResponse> deptData = complaintRepository.getDepartmentPerformance();
        List<Map<String, Object>> deptPerf = new ArrayList<>();
        for (DepartmentPerformanceResponse dept : deptData) {
            Map<String, Object> entry = new LinkedHashMap<>();

            long deptTotal    = dept.getOpenComplaints() + dept.getResolvedComplaints();
            long deptResolved = dept.getResolvedComplaints();
            long deptOpen     = dept.getOpenComplaints();
            double sla        = deptTotal > 0
                    ? Math.round(((double) deptResolved / deptTotal) * 100.0)
                    : 0;

            entry.put("department",    dept.getDepartmentName());
            entry.put("total",         deptTotal);
            entry.put("resolved",      deptResolved);
            entry.put("inProgress",    deptOpen);
            entry.put("pending",       deptOpen);
            entry.put("slaCompliance", sla);
            deptPerf.add(entry);
        }
        stats.put("departmentPerformance", deptPerf);

        // ── Recent complaints ─────────────────────────────────────────────────
        List<Complaint> recent = complaintRepository.findTop10ByOrderByCreatedAtDesc();
        List<Map<String, Object>> recentList = new ArrayList<>();
        for (Complaint c : recent) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",              c.getId());
            entry.put("referenceNumber", c.getReferenceNumber());
            entry.put("category",        c.getCategory());
            entry.put("area",            c.getArea());
            entry.put("status",          c.getStatus());
            entry.put("priority",        c.getPriority());
            entry.put("assignedTo",      c.getAssignedTo());
            entry.put("createdAt",       c.getCreatedAt());
            recentList.add(entry);
        }
        stats.put("recentComplaints", recentList);

        // ── Complaints by area ────────────────────────────────────────────────
        List<Object[]> areaData = complaintRepository.getComplaintsByArea();
        List<Map<String, Object>> areaList = new ArrayList<>();
        for (Object[] row : areaData) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("area",  row[0]);
            entry.put("count", row[1]);
            areaList.add(entry);
        }
        stats.put("complaintsByArea", areaList);

        return stats;
    }
}
