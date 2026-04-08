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

    // Falls back to "admin123" if not set in application.properties
    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository) {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User admin = userRepository.findByEmail(adminEmail);

            if (admin == null) {
                admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(encoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setFullName("System Administrator");
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("✅ Admin user created: " + adminEmail);
            } else {
                boolean needsUpdate = false;

                if (!"ADMIN".equals(admin.getRole())) {
                    admin.setRole("ADMIN");
                    needsUpdate = true;
                }

                if (!Boolean.TRUE.equals(admin.getActive())) {
                    needsUpdate = true;
                }

                if (!encoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(encoder.encode(adminPassword));
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    userRepository.save(admin);
                    System.out.println("✅ Admin user updated: " + adminEmail);
                } else {
                    System.out.println("✅ Admin user already correct: " + adminEmail);
                }
            }
        };
    }
}