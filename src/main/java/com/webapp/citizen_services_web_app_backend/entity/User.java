package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;                    // CITIZEN, STAFF, ADMIN

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Role roleEnum;

    @Column
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiry;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Complaint> complaints = new ArrayList<>();

    private String phone;
    private String fullName;
    private String address;
    private String ward;
    private String profileImageUrl;

    @Column(nullable = false)
    private Boolean active = true;          // Changed to Boolean (recommended)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Lombok + Boolean will generate both isActive() and getActive()
}