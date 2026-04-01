package com.webapp.citizen_services_web_app_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {
    private String email;
    private String phone;
    private String address;
    private String ward;
    private String profileImageUrl;
}
