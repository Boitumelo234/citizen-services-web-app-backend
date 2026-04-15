package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Added
import java.util.List;
import java.util.Map; // Added

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findAllByOrderByCreatedAtDesc();


    @Query("SELECT c.category AS category, " +
            "SUM(CASE WHEN LOWER(c.status) = 'in progress' THEN 1 ELSE 0 END) AS active, " +
            "SUM(CASE WHEN LOWER(c.status) = 'pending' THEN 1 ELSE 0 END) AS pending, " +
            "SUM(CASE WHEN LOWER(c.status) = 'resolved' THEN 1 ELSE 0 END) AS resolved, " +
            "SUM(CASE WHEN LOWER(c.status) = 'rejected' THEN 1 ELSE 0 END) AS rejected " +
            "FROM Complaint c GROUP BY c.category")
    List<Map<String, Object>> getDepartmentStats();
}