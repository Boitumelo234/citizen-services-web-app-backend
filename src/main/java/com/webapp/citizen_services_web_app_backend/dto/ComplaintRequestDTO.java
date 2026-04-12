// ComplaintRequestDTO.java
package com.webapp.citizen_services_web_app_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
package com.webapp.citizen_services_web_app_backend.dto;

import lombok.Data;

@Data
public class ComplaintRequestDTO {
    @NotBlank(message = "Category is required")
    private String category;

    private String location;

    @NotBlank(message = "Description is required")
    private String description;

    private String priority;
    private Double latitude;
    private Double longitude;
}