package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user by their email address (used for login/registration)
    User findByEmail(String email);

    // NEW: Finds a user by their secret reset token
    // This allows the /api/auth/reset-password endpoint to identify the user
    User findByResetToken(String resetToken);
}