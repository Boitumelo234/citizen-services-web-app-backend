package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // NEW: Finds a user by their secret reset token
    // This allows the /api/auth/reset-password endpoint to identify the user
    User findByResetToken(String resetToken);

    long countByRole(Role role);  // ✅ ENUM TYPE

    long countByActiveTrue();
    long countByActiveFalse();
}