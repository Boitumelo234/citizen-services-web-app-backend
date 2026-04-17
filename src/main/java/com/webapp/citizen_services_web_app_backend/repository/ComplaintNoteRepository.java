package com.webapp.citizen_services_web_app_backend.repository;

import com.webapp.citizen_services_web_app_backend.entity.ComplaintNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintNoteRepository extends JpaRepository<ComplaintNote, Long> {

    /**
     * Fetch all notes for a complaint, oldest first.
     * Uses the ManyToOne complaint.id (not a direct complaintId column).
     */
    @Query("SELECT n FROM ComplaintNote n WHERE n.complaint.id = :complaintId ORDER BY n.createdAt ASC")
    List<ComplaintNote> findByComplaintIdOrderByCreatedAtAsc(@Param("complaintId") Long complaintId);
}