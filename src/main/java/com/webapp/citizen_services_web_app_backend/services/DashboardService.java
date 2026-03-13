package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.DashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProfileService profileService;
    private final NotificationService notificationService;

    public DashboardDTO getDashboard(String email) {
        String citizenName = profileService.getProfile(email).getEmail();
        if (citizenName != null && citizenName.contains("@")) {
            citizenName = citizenName.substring(0, citizenName.indexOf('@'));
        }

        return DashboardDTO.builder()
                .citizenName(citizenName)
                .totalComplaints(0)
                .resolvedThisMonth(0)
                .unreadNotifications(notificationService.countUnread(email))
                .categories(List.of())
                .recentComplaints(List.of())
                .build();
    }
}
