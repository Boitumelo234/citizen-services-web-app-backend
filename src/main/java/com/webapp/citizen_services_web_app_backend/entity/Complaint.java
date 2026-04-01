package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String referenceNumber;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User citizen;

    @Column
    private String assignedTo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private LocalDateTime updatedAt;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Complaint() {}

    public Complaint(Long id, String referenceNumber, String category,
                     String description, String status, String priority,
                     String area, User citizen, String assignedTo,
                     LocalDateTime createdAt, LocalDateTime resolvedAt,
                     LocalDateTime updatedAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.category = category;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.area = area;
        this.citizen = citizen;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.updatedAt = updatedAt;
    }

    // ─── Lifecycle hooks ──────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null)   this.status   = "New";
        if (this.priority == null) this.priority = "Medium";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getReferenceNumber()   { return referenceNumber; }
    public String getCategory()          { return category; }
    public String getDescription()       { return description; }
    public String getStatus()            { return status; }
    public String getPriority()          { return priority; }
    public String getArea()              { return area; }
    public User getCitizen()             { return citizen; }
    public String getAssignedTo()        { return assignedTo; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // ─── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id)                          { this.id = id; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public void setCategory(String category)            { this.category = category; }
    public void setDescription(String description)      { this.description = description; }
    public void setStatus(String status)                { this.status = status; }
    public void setPriority(String priority)            { this.priority = priority; }
    public void setArea(String area)                    { this.area = area; }
    public void setCitizen(User citizen)                { this.citizen = citizen; }
    public void setAssignedTo(String assignedTo)        { this.assignedTo = assignedTo; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)   { this.updatedAt = updatedAt; }
}

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