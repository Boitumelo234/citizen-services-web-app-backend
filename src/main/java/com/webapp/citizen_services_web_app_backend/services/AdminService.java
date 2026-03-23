package com.webapp.citizen_services_web_app_backend.services;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse;
import com.webapp.citizen_services_web_app_backend.dto.DashboardResponse;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.dto.UserOverviewResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import java.util.List;


@Service
public class AdminService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public AdminService(ComplaintRepository complaintRepository,
                    UserRepository userRepository) {
    this.complaintRepository = complaintRepository;
    this.userRepository = userRepository;
}

    public DashboardResponse getDashboardData() {

        long newToday = complaintRepository
                .countByCreatedAtAfter(LocalDateTime.now().minusDays(1));

        long totalOpen = complaintRepository.countByStatus("SUBMITTED")
                + complaintRepository.countByStatus("ASSIGNED")
                + complaintRepository.countByStatus("IN_PROGRESS");

        long totalResolved = complaintRepository.countByStatus("RESOLVED");

        Double avgTime = complaintRepository.averageResolutionTime();
        double averageResolution = avgTime != null ? avgTime : 0.0;

        return new DashboardResponse(
                newToday,
                totalOpen,
                totalResolved,
                averageResolution
        );
    }

public List<DepartmentPerformanceResponse> getDepartmentPerformance() {
    return complaintRepository.getDepartmentPerformance();
}
public List<ComplaintTrendResponse> getComplaintTrend() {
    return complaintRepository.getComplaintTrend();
}

public UserOverviewResponse getUserOverview() {

    long total = userRepository.count();
    long adminCount = userRepository.countByRole(Role.ADMIN);
    long agentCount = userRepository.countByRole(Role.STAFF);
    long citizenCount = userRepository.countByRole(Role.CITIZEN);
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
