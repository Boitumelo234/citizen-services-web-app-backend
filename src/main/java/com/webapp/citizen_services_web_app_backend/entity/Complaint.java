package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
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
