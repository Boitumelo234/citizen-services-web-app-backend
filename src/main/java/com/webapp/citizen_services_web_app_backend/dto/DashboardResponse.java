package com.webapp.citizen_services_web_app_backend.dto;

public class DashboardResponse {

    private long newComplaintsToday;
    private long totalOpen;
    private long totalResolved;
    private double avgResolutionTime;

    public DashboardResponse(long newComplaintsToday,
                             long totalOpen,
                             long totalResolved,
                             double avgResolutionTime) {
        this.newComplaintsToday = newComplaintsToday;
        this.totalOpen = totalOpen;
        this.totalResolved = totalResolved;
        this.avgResolutionTime = avgResolutionTime;
    }

    public long getNewComplaintsToday() {
        return newComplaintsToday;
    }

    public long getTotalOpen() {
        return totalOpen;
    }

    public long getTotalResolved() {
        return totalResolved;
    }

    public double getAvgResolutionTime() {
        return avgResolutionTime;
    }
}