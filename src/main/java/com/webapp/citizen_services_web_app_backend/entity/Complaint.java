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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_number", unique = true, nullable = false, length = 20)
    private String referenceNumber;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String location;   // ← human readable (keep it)

    // ── NEW FIELDS FOR MAP ───────────────────────────────────────
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
    // ─────────────────────────────────────────────────────────────

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

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ComplaintUpdate> updates = new ArrayList<>();

    // Add to Complaint.java entity
    @Column(length = 20)
    private String priority = "medium";

    @PrePersist
    protected void onCreate() {
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