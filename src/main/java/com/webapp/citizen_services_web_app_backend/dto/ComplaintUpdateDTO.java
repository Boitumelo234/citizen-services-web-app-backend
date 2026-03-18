package com.webapp.citizen_services_web_app_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComplaintUpdateDTO {
    private Long id;
    private String comment;
    private String newLocation;
    private String photoUrl;
    private LocalDateTime createdAt;
}