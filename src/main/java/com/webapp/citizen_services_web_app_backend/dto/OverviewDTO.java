package com.webapp.citizen_services_web_app_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverviewDTO {
    private long lifetimeSubmitted;
    private long resolved;
    private long open;
    private double avgResolutionDays;
    private List<DashboardDTO.CategoryCount> topCategories;
    private List<MonthlyTrend> monthlyTrend;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyTrend {
        private String month;
        private long count;
    }
}
