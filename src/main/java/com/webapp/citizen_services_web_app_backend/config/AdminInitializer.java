package com.webapp.citizen_services_web_app_backend.config;

import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AdminInitializer {

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            User admin = userRepository.findByEmail(adminEmail);

            if (admin == null) {
                // Create new admin
                admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                System.out.println("Admin user created: " + adminEmail + " with password from properties");
            } else {
                // Always enforce correct role and password
                boolean needsUpdate = false;

                if (!"ADMIN".equals(admin.getRole())) {
                    admin.setRole("ADMIN");
                    needsUpdate = true;
                }

                // Force password reset to ensure it's BCrypt (uncommented)
                String newHash = passwordEncoder.encode(adminPassword);
                if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(newHash);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    userRepository.save(admin);
                    System.out.println("Admin user updated: " + adminEmail);
                } else {
                    System.out.println("Admin user already correct: " + adminEmail);
                }
            }
        };
    }
}