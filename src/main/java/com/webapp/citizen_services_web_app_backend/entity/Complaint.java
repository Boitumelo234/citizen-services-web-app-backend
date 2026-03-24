package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "complaints")
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
    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ComplaintUpdate> updates = new ArrayList<>();

    // 🔹 Auto-fill before insert
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
        if (this.referenceNumber == null || this.referenceNumber.isEmpty()) {
            this.referenceNumber = "RUST-" + (1000 + (int)(Math.random() * 9000));
        }

        // Only set "Pending" if the status wasn't already
        // manually set by the admin or a controller.
        if (this.status == null || this.status.isEmpty()) {
            this.status = "Pending";
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