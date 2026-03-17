package com.webapp.citizen_services_web_app_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintResponseDTO {

    private Long id;
    private String referenceNumber;
    private String category;
    private String location;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private String photoUrl;          // null for now

    // You can add user email or other info later if needed
}