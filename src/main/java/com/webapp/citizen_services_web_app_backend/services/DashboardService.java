package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.DashboardDTO;
import com.webapp.citizen_services_web_app_backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ComplaintService complaintService;
    private final ProfileService profileService;
    private final NotificationService notificationService;

    public DashboardDTO getDashboard(String email) {
        User user = complaintService.getCurrentUserByEmail(email);
        String citizenName = deriveCitizenName(profileService.getProfile(email).getEmail());
        List<DashboardDTO.CategoryCount> categories = complaintService.getCategoryCounts(user);

        return DashboardDTO.builder()
                .citizenName(citizenName)
                .totalComplaints(complaintService.countComplaints(user))
                .resolvedThisMonth(complaintService.countResolvedThisMonth(user))
                .unreadNotifications(notificationService.countUnread(email))
                .categories(categories.isEmpty() ? defaultCategories() : categories)
                .recentComplaints(complaintService.getRecentComplaints(user))
                .build();
    }

    private String deriveCitizenName(String email) {
        if (email == null || email.isBlank()) {
            return "Citizen";
        }

        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (localPart.isBlank()) {
            return "Citizen";
        }

        String normalized = localPart.replace('.', ' ').replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private List<DashboardDTO.CategoryCount> defaultCategories() {
        return List.of(
                DashboardDTO.CategoryCount.builder().name("Infrastructure & Roads").count(0).build(),
                DashboardDTO.CategoryCount.builder().name("Water & Sanitation").count(0).build(),
                DashboardDTO.CategoryCount.builder().name("Electricity & Energy").count(0).build()
        );
    }
}
