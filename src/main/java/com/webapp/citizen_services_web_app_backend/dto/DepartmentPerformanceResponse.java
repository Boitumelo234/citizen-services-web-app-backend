package com.webapp.citizen_services_web_app_backend.dto;

public class DepartmentPerformanceResponse {

    private String departmentName;
    private Long openComplaints;
    private Long resolvedComplaints;

    public DepartmentPerformanceResponse(String departmentName,
                                         Long openComplaints,
                                         Long resolvedComplaints) {
        this.departmentName = departmentName;
        this.openComplaints = openComplaints;
        this.resolvedComplaints = resolvedComplaints;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Long getOpenComplaints() {
        return openComplaints;
    }

    public Long getResolvedComplaints() {
        return resolvedComplaints;
    }
}