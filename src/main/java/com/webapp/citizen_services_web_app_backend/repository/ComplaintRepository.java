package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

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
