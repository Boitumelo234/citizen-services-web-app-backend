package com.webapp.citizen_services_web_app_backend.dto;

import lombok.Data;

@Data
public class ComplaintRequestDTO {
    private String category;

    private String location;

    private String description;

    private String priority;
    private Double latitude;
    private Double longitude;
}
