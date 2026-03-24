package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    // NEW: Fetch every complaint in the system for the Admin
    List<Complaint> findAllByOrderByCreatedAtDesc();
}