package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // TRANSPORT, WATER, ELECTRICITY, WASTE

    private String description;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<User> staff;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Complaint> complaints;
}