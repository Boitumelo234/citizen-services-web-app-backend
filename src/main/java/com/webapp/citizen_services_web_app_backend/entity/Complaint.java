package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference number (unique)
    @Column(name = "reference_number", unique = true, nullable = false, length = 32)
    private String referenceNumber;

    // Title (from branch)
    @Column(nullable = true)
    private String title;

    // Category
    @Column(nullable = false, length = 120)
    private String category;

    // Location (from main)
    @Column(nullable = false, length = 255)
    private String location;

    // Area (from branch)
    private String area;

    // Description
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Photo URL
    @Column(name = "photo_url")
    private String photoUrl;

    // Status (default Pending)
    @Column(nullable = false, length = 40)
    private String status = "Pending";

    // Priority (default medium)
    @Column(nullable = false, length = 20)
    private String priority = "medium";

    // Coordinates
    private Double latitude;
    private Double longitude;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Assignment (String version – used in some places)
    @Column(name = "assigned_to")
    private String assignedTo;

    // Assignment (User version – used in main branch)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedToUser;

    // Department
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Citizen (branch version)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    // User (main version)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Updates list
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ComplaintUpdate> updates = new ArrayList<>();

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (submittedAt == null) submittedAt = now;
        if (updatedAt == null) updatedAt = now;
        if (referenceNumber == null || referenceNumber.isBlank()) {
            referenceNumber = "RUST-" + (1000 + (int)(Math.random() * 9000));
        }
        if (status == null || status.isBlank()) status = "Pending";
        if (priority == null || priority.isBlank()) priority = "medium";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if ("RESOLVED".equalsIgnoreCase(status) && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + id +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", category='" + category + '\'' +
                ", status='" + status + '\'' +
                ", updatesCount=" + updates.size() +
                '}';
    }
}