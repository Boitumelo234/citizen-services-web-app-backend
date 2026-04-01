package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.SystemSettings;
import com.webapp.citizen_services_web_app_backend.repository.SystemSettingsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class SystemSettingsController {

    private final SystemSettingsRepository repository;

    public SystemSettingsController(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    // GET current system settings
    @GetMapping
    public SystemSettings getSettings() {
        return repository.findById(1L)
                .orElseGet(() -> {
                    SystemSettings s = new SystemSettings();
                    s.setId(1L); // always use ID = 1
                    s.setAutoRoutingEnabled(true);
                    s.setAdminEmailNotifications(true);
                    return repository.save(s);
                });
    }

    // UPDATE system settings
    @PutMapping
    public SystemSettings updateSettings(@RequestBody SystemSettings settings) {
        settings.setId(1L); // always update the single row
        return repository.save(settings);
    }
}