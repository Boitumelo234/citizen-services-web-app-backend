package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByDepartmentId(Long departmentId);
    List<User> findByRoleAndDepartmentId(String role, Long departmentId);
    long countByRole(String role);
    long countByActive(Boolean active);
}