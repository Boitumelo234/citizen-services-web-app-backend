package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.OverviewDTO;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
public class OverviewService {

    public OverviewDTO getOverview(String email) {
        return OverviewDTO.builder()
                .lifetimeSubmitted(0)
                .resolved(0)
                .open(0)
                .avgResolutionDays(0)
                .topCategories(defaultTopCategories())
                .monthlyTrend(defaultMonthlyTrend())
                .build();
    }

    private List<OverviewDTO.MonthlyTrend> defaultMonthlyTrend() {
        return IntStream.rangeClosed(0, 5)
                .mapToObj(offset -> YearMonth.now().minusMonths(5L - offset))
                .map(month -> OverviewDTO.MonthlyTrend.builder()
                        .month(month.getMonth().name().substring(0, 3).toUpperCase(Locale.ROOT))
                        .count(0)
                        .build())
                .toList();
    }

    private List<com.webapp.citizen_services_web_app_backend.dto.DashboardDTO.CategoryCount> defaultTopCategories() {
        return List.of(
                com.webapp.citizen_services_web_app_backend.dto.DashboardDTO.CategoryCount.builder()
                        .name("Infrastructure & Roads")
                        .count(0)
                        .build(),
                com.webapp.citizen_services_web_app_backend.dto.DashboardDTO.CategoryCount.builder()
                        .name("Water & Sanitation")
                        .count(0)
                        .build(),
                com.webapp.citizen_services_web_app_backend.dto.DashboardDTO.CategoryCount.builder()
                        .name("Electricity & Energy")
                        .count(0)
                        .build()
        );
    }
}
