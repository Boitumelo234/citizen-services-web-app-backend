package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse;
import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ==================== Methods from Main Branch ====================
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    Complaint findByReferenceNumber(String referenceNumber);

    @Query("SELECT c FROM Complaint c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Complaint> findAllWithCoordinates();

    long countByStatus(String status);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :startOfMonth")
    long countNewThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.resolvedAt >= :startOfMonth")
    long countResolvedThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :weekStart")
    long countComplaintsThisWeek(LocalDateTime weekStart);

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.resolvedAt)) FROM Complaint c WHERE c.resolvedAt IS NOT NULL")
    Double getAvgResolutionTimeHours();

    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.resolvedAt)) / 24.0 FROM Complaint c WHERE c.resolvedAt IS NOT NULL")
    Double averageResolutionTime();

    @Query("SELECT new com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse(" +
            "c.category, " +
            "SUM(CASE WHEN c.status != 'Resolved' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN c.status = 'Resolved' THEN 1 ELSE 0 END)) " +
            "FROM Complaint c GROUP BY c.category")
    List<DepartmentPerformanceResponse> getDepartmentPerformance();

    @Query("SELECT new com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse(" +
            "CAST(c.createdAt AS date), COUNT(c)) " +
            "FROM Complaint c " +
            "GROUP BY CAST(c.createdAt AS date) " +
            "ORDER BY CAST(c.createdAt AS date) ASC")
    List<ComplaintTrendResponse> getComplaintTrend();
    // ✅ Replace with this:
    @Query("SELECT c FROM Complaint c WHERE c.assignedTo = :staffId")
    List<Complaint> findByAssignedToId(@Param("staffId") Long staffId);

    // ==================== Methods from Your Branch ====================
    List<Complaint> findByStatus(String status);

    List<Complaint> findByPriority(String priority);

    List<Complaint> findByCategory(String category);

//    List<Complaint> findByAssignedToId(Long staffId);

    List<Complaint> findByCitizenId(Long citizenId);

    List<Complaint> findByDepartmentId(Long departmentId);

    List<Complaint> findByAssignedToIsNull();

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

    // ==================== Commented / Duplicate methods from main (preserved) ====================
    // List<Complaint> findByUserId(Long userId);
    // List<Complaint> findTop10ByOrderByCreatedAtDesc();
    // long countByCategory(String category);   // already present from branch
    // @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    // List<Object[]> getStatusCounts();
    // @Query("SELECT c.area, COUNT(c) FROM Complaint c GROUP BY c.area ORDER BY COUNT(c) DESC")
    // List<Object[]> getComplaintsByArea();
}
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findTop5ByUserOrderByCreatedAtDesc(User user);

    List<Complaint> findByUser(User user);

    long countByUser(User user);

    long countByUserAndStatusIgnoreCase(User user, String status);

    @Query("""
        select count(c) from Complaint c
        where c.user = :user
        and lower(c.status) = lower(:status)
        and c.resolvedAt >= :startOfMonth
        """)
    long countResolvedByUserSince(User user, String status, LocalDateTime startOfMonth);

    @Query("""
        select c from Complaint c
        where c.latitude is not null and c.longitude is not null
        order by c.createdAt desc
        """)
    List<Complaint> findAllWithCoordinates();
}
