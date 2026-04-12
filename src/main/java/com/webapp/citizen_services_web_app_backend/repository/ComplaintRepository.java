package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse;
import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ==================== Methods from Main Branch ====================
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    Complaint findByReferenceNumber(String referenceNumber);

    List<Complaint> findTop5ByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findByUser(User user);

    long countByUser(User user);

    long countByUserAndStatusIgnoreCase(User user, String status);

    @Query("""
        SELECT COUNT(c) FROM Complaint c
        WHERE c.user = :user
          AND LOWER(c.status) = LOWER(:status)
          AND c.resolvedAt >= :startOfMonth
    """)
    long countResolvedByUserSince(@Param("user") User user,
                                  @Param("status") String status,
                                  @Param("startOfMonth") LocalDateTime startOfMonth);

    long countByStatus(String status);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :startOfMonth")
    long countNewThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.resolvedAt >= :startOfMonth")
    long countResolvedThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :weekStart")
    long countComplaintsThisWeek(@Param("weekStart") LocalDateTime weekStart);

    // ✅ FIXED: Native queries for TIMESTAMPDIFF
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(HOUR, c.created_at, c.resolved_at)) FROM complaints c WHERE c.resolved_at IS NOT NULL", nativeQuery = true)
    Double getAvgResolutionTimeHours();

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(HOUR, c.created_at, c.resolved_at)) / 24.0 FROM complaints c WHERE c.resolved_at IS NOT NULL", nativeQuery = true)
    Double averageResolutionTime();

    @Query("""
        SELECT new com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse(
            c.category,
            SUM(CASE WHEN c.status <> 'Resolved' THEN 1 ELSE 0 END),
            SUM(CASE WHEN c.status = 'Resolved' THEN 1 ELSE 0 END)
        )
        FROM Complaint c
        GROUP BY c.category
    """)
    List<DepartmentPerformanceResponse> getDepartmentPerformance();

    @Query("""
        SELECT new com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse(
            CAST(c.createdAt AS date),
            COUNT(c)
        )
        FROM Complaint c
        GROUP BY CAST(c.createdAt AS date)
        ORDER BY CAST(c.createdAt AS date) ASC
    """)
    List<ComplaintTrendResponse> getComplaintTrend();

    // ==================== Methods from Your Branch ====================
    List<Complaint> findByStatus(String status);

    List<Complaint> findByPriority(String priority);

    List<Complaint> findByCategory(String category);

    @Query("SELECT c FROM Complaint c WHERE c.assignedToUser.id = :staffId")
    List<Complaint> findByAssignedToId(@Param("staffId") Long staffId);

    List<Complaint> findByCitizenId(Long citizenId);

    List<Complaint> findByDepartmentId(Long departmentId);

    List<Complaint> findByAssignedToIsNull();

    long countByCategory(String category);

    long countByDepartmentId(Long departmentId);

    @Query("SELECT c FROM Complaint c WHERE c.createdAt >= :since")
    List<Complaint> findSince(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Complaint c WHERE c.status = 'RESOLVED' AND c.resolvedAt >= :since")
    List<Complaint> findResolvedSince(@Param("since") LocalDateTime since);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM Complaint c WHERE c.createdAt >= :since GROUP BY DATE(c.createdAt) ORDER BY DATE(c.createdAt)")
    List<Object[]> dailyComplaintCounts(@Param("since") LocalDateTime since);

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED') AND c.createdAt <= :threshold ORDER BY c.priority DESC, c.createdAt ASC")
    List<Complaint> findOverdueComplaints(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT c FROM Complaint c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL ORDER BY c.createdAt DESC")
    List<Complaint> findAllWithCoordinates();
}