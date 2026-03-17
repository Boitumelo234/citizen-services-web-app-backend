package com.webapp.citizen_services_web_app_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequestDTO {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Description is required")
    private String description;

    // photoUrl is optional → can be added later when implementing file upload
}