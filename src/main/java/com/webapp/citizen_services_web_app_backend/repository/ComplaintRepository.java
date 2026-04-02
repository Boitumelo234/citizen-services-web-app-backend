package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse;
import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Change this from findByCitizenId to findByUserId
    List<Complaint> findByUserId(Long userId);

    List<Complaint> findByStatus(String status);

    List<Complaint> findByCategory(String category);

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    List<Complaint> findAllByOrderByCreatedAtDesc();

    Complaint findByReferenceNumber(String referenceNumber);

    @Query("SELECT c FROM Complaint c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Complaint> findAllWithCoordinates();

    long countByStatus(String status);

    long countByCategory(String category);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :startOfMonth")
    long countNewThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.resolvedAt >= :startOfMonth")
    long countResolvedThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :weekStart")
    long countComplaintsThisWeek(LocalDateTime weekStart);

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> getStatusCounts();

    @Query("SELECT c.area, COUNT(c) FROM Complaint c GROUP BY c.area ORDER BY COUNT(c) DESC")
    List<Object[]> getComplaintsByArea();

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
}