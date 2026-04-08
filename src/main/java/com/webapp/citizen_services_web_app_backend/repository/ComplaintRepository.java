package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(String status);

    List<Complaint> findByPriority(String priority);

    List<Complaint> findByCategory(String category);

    List<Complaint> findByAssignedToId(Long staffId);

    List<Complaint> findByCitizenId(Long citizenId);

    List<Complaint> findByDepartmentId(Long departmentId);

    List<Complaint> findByAssignedToIsNull();

    long countByStatus(String status);

    long countByCategory(String category);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT c FROM Complaint c WHERE c.createdAt >= :since")
    List<Complaint> findSince(LocalDateTime since);

    @Query("SELECT c FROM Complaint c WHERE c.status = 'RESOLVED' AND c.resolvedAt >= :since")
    List<Complaint> findResolvedSince(LocalDateTime since);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM Complaint c WHERE c.createdAt >= :since GROUP BY DATE(c.createdAt) ORDER BY DATE(c.createdAt)")
    List<Object[]> dailyComplaintCounts(LocalDateTime since);

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED') AND c.createdAt <= :threshold ORDER BY c.priority DESC, c.createdAt ASC")
    List<Complaint> findOverdueComplaints(LocalDateTime threshold);
}