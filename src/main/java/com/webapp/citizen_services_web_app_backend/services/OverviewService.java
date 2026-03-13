package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.OverviewDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OverviewService {

    public OverviewDTO getOverview(String email) {
        return OverviewDTO.builder()
                .lifetimeSubmitted(0)
                .resolved(0)
                .open(0)
                .avgResolutionDays(0)
                .topCategories(List.of())
                .monthlyTrend(List.of())
                .build();
    }
}
