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

    // Fields from main branch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_number", unique = true, nullable = false, length = 20)
    private String referenceNumber;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false, length = 20)
    private String status = "Pending";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "area")
    private String area;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ComplaintUpdate> updates = new ArrayList<>();

    @Column(length = 20)
    private String priority = "medium";

    // Fields from your branch (without removing anything)
    @Column(nullable = false)
    private String title;

    // Branch also has "area", "priority", "status", "latitude", "longitude", "description", "resolvedAt", "createdAt"
    // These are already covered above with main branch columns (we keep the column definitions from main where they conflict)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedToUser;   // renamed to avoid conflict with String assignedTo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // PrePersist and PreUpdate combined (both logics preserved)
    @PrePersist
    protected void onCreate() {
        // From main branch
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (referenceNumber == null || referenceNumber.isEmpty()) {
            referenceNumber = "RUST-" + (1000 + (int)(Math.random() * 9000));
        }
        if (status == null) {
            status = "Pending";
        }

        // From your branch
        if (status == null) status = "PENDING";
        if (priority == null) priority = "MEDIUM";
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // From your branch
        updatedAt = LocalDateTime.now();
        if ("RESOLVED".equals(status) && resolvedAt == null) {
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