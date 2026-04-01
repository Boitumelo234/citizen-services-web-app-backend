package com.webapp.citizen_services_web_app_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private String citizenName;
    private long totalComplaints;
    private long resolvedThisMonth;
    private long unreadNotifications;
    private List<CategoryCount> categories;
    private List<RecentComplaint> recentComplaints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryCount {
        private String name;
        private long count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentComplaint {
        private String id;
        private String title;
        private String category;
        private String status;
        private String date;
    }
}
