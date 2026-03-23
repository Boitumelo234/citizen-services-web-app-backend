package com.webapp.citizen_services_web_app_backend.dto;

public class UserOverviewResponse {

    private long totalUsers;
    private long admins;
    private long agents;
    private long departmentUsers;
    private long activeUsers;
    private long inactiveUsers;

    public UserOverviewResponse(long totalUsers,
                                long admins,
                                long agents,
                                long departmentUsers,
                                long activeUsers,
                                long inactiveUsers) {
        this.totalUsers = totalUsers;
        this.admins = admins;
        this.agents = agents;
        this.departmentUsers = departmentUsers;
        this.activeUsers = activeUsers;
        this.inactiveUsers = inactiveUsers;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getAdmins() { return admins; }
    public long getAgents() { return agents; }
    public long getDepartmentUsers() { return departmentUsers; }
    public long getActiveUsers() { return activeUsers; }
    public long getInactiveUsers() { return inactiveUsers; }
}