package com.webapp.citizen_services_web_app_backend.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;   // ✅ use enum Role instead of String

    private boolean active = true;

public boolean isActive() {
    return active;
}

public void setActive(boolean active) {
    this.active = active;
}



}