package com.webapp.citizen_services_web_app_backend.config;

import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.entity.Role;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class AdminInitializer {

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder) {

        return args -> {

            Optional<User> optionalAdmin = userRepository.findByEmail(adminEmail);

            if (optionalAdmin.isEmpty()) {

                // Create new admin
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);

                userRepository.save(admin);

                System.out.println("Admin user created: " + adminEmail);

            } else {

                User admin = optionalAdmin.get();
                boolean needsUpdate = false;

                // Ensure role is ADMIN
                if (admin.getRole() != Role.ADMIN) {
                    admin.setRole(Role.ADMIN);
                    needsUpdate = true;
                }

                // Ensure password matches property
                if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
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