package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse;
import com.webapp.citizen_services_web_app_backend.dto.DashboardResponse;
import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.dto.UserOverviewResponse;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public AdminService(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    // ==================== Your Branch Version (Primary) ====================
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new LinkedHashMap<>();

        long total = complaintRepository.count();
        long pending = complaintRepository.countByStatus("PENDING");
        long assigned = complaintRepository.countByStatus("ASSIGNED");
        long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
        long resolved = complaintRepository.countByStatus("RESOLVED");
        long open = pending + assigned + inProgress;

        // Users — role is now a plain String
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole("ADMIN");
        long staffCount = userRepository.countByRole("STAFF");
        long citizenCount = userRepository.countByRole("CITIZEN");
        long activeUsers = userRepository.countByActive(true);

        // Resolved today
        long resolvedToday = complaintRepository
                .findResolvedSince(LocalDateTime.now().toLocalDate().atStartOfDay())
                .size();

        data.put("totalComplaints", total);
        data.put("openComplaints", open);
        data.put("resolvedToday", resolvedToday);
        data.put("pendingComplaints", pending);
        data.put("inProgress", inProgress);
        data.put("totalUsers", totalUsers);
        data.put("adminCount", adminCount);
        data.put("staffCount", staffCount);
        data.put("citizenCount", citizenCount);
        data.put("activeUsers", activeUsers);

        return data;
    }

    // ==================== Additional Methods from Main Branch ====================
    public List<DepartmentPerformanceResponse> getDepartmentPerformance() {
        return complaintRepository.getDepartmentPerformance();
    }

    public List<ComplaintTrendResponse> getComplaintTrend() {
        return complaintRepository.getComplaintTrend();
    }

    public UserOverviewResponse getUserOverview() {
        long total = userRepository.count();
        long adminCount = userRepository.countByRole("ADMIN");
        long agentCount = userRepository.countByRole("STAFF");
        long citizenCount = userRepository.countByRole("CITIZEN");
        long active = userRepository.countByActiveTrue();
        long inactive = userRepository.countByActiveFalse();

        return new UserOverviewResponse(
                total,
                adminCount,
                agentCount,
                citizenCount,
                active,
                inactive
        );
    }
}