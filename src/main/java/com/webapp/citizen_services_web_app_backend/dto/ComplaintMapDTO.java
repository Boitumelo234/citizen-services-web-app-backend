package com.webapp.citizen_services_web_app_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintMapDTO {

    private Long id;
    private String referenceNumber;
    private String category;
    private String status;
    private String priority;
    private String location;
    private Double latitude;
    private Double longitude;
    private String description;
    private String photoUrl;
    private LocalDateTime submittedAt;

    // Optional: you can add priority logic later if you introduce a priority field
}