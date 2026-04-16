package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSettings {  // ✅ Uppercase S's

    @Id
    private Long id = 1L;

    private boolean autoRoutingEnabled;
    private boolean adminEmailNotifications;

    public SystemSettings() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {  // ✅ This method must exist
        this.id = id;
    }

    public boolean isAutoRoutingEnabled() { return autoRoutingEnabled; }
    public void setAutoRoutingEnabled(boolean autoRoutingEnabled) { this.autoRoutingEnabled = autoRoutingEnabled; }

    public boolean isAdminEmailNotifications() { return adminEmailNotifications; }
    public void setAdminEmailNotifications(boolean adminEmailNotifications) { this.adminEmailNotifications = adminEmailNotifications; }
}
