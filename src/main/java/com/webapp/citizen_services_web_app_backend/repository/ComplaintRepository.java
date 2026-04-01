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

    // ─── Basic finders ────────────────────────────────────────────────────────
    List<Complaint> findByCitizenId(Long citizenId);
    List<Complaint> findByStatus(String status);
    List<Complaint> findByCategory(String category);
    List<Complaint> findTop10ByOrderByCreatedAtDesc();
    List<Complaint> findAllByOrderByCreatedAtDesc();
    Complaint findByReferenceNumber(String referenceNumber);

    // ─── Count queries ────────────────────────────────────────────────────────
    long countByStatus(String status);
    long countByCategory(String category);
    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :startOfMonth")
    long countNewThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.resolvedAt >= :startOfMonth")
    long countResolvedThisMonth(LocalDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :weekStart")
    long countComplaintsThisWeek(LocalDateTime weekStart);

    // ─── Status distribution (used by ComplaintService) ───────────────────────
    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> getStatusCounts();

    // ─── Complaints by area (used by ComplaintService) ────────────────────────
    @Query("SELECT c.area, COUNT(c) FROM Complaint c GROUP BY c.area ORDER BY COUNT(c) DESC")
    List<Object[]> getComplaintsByArea();

    // ─── Average resolution time in hours (used by ComplaintService) ──────────
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.resolvedAt)) FROM Complaint c WHERE c.resolvedAt IS NOT NULL")
    Double getAvgResolutionTimeHours();

    // ─── Average resolution time in days (used by AdminService) ──────────────
    // This satisfies AdminService.averageResolutionTime()
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, c.createdAt, c.resolvedAt)) / 24.0 FROM Complaint c WHERE c.resolvedAt IS NOT NULL")
    Double averageResolutionTime();

    // ─── Department performance returning DTO (used by AdminService) ──────────
    // Returns List<DepartmentPerformanceResponse> directly so AdminService gets
    // the correct type instead of List<Object[]>
    @Query("SELECT new com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse(" +
           "c.category, " +
           "SUM(CASE WHEN c.status != 'Resolved' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN c.status = 'Resolved' THEN 1 ELSE 0 END)) " +
           "FROM Complaint c GROUP BY c.category")
    List<DepartmentPerformanceResponse> getDepartmentPerformance();

    // ─── Complaint trend by date (used by AdminService) ───────────────────────
    // Groups complaints by date for trend chart
    @Query("SELECT new com.webapp.citizen_services_web_app_backend.dto.ComplaintTrendResponse(" +
           "CAST(c.createdAt AS date), COUNT(c)) " +
           "FROM Complaint c " +
           "GROUP BY CAST(c.createdAt AS date) " +
           "ORDER BY CAST(c.createdAt AS date) ASC")
    List<ComplaintTrendResponse> getComplaintTrend();
}

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT c FROM Complaint c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Complaint> findAllWithCoordinates();
}