package com.webapp.citizen_services_web_app_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ComplaintResponseDTO {
    private Long id;
    private String referenceNumber;
    private String category;
    private String location;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private String photoUrl;

    private String assignedToEmail;
    private String assignedToName;
    private String title;
    private String area;
    private String priority;

    private List<ComplaintUpdateDTO> updates = new ArrayList<>();
}