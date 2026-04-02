package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.ProfileDTO;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileDTO getProfile(String email) {
        User user = getCurrentUser(email);
        return toDto(user);
    }

    public ProfileDTO updateProfile(String email, ProfileDTO dto) {
        User user = getCurrentUser(email);
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setWard(dto.getWard());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        userRepository.save(user);
        return toDto(user);
    }

    private User getCurrentUser(String email) {
        // Updated to use the Optional return from repository
        // orElseThrow automatically unwraps the User or throws the exception if empty
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    private ProfileDTO toDto(User user) {
        return ProfileDTO.builder()
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .ward(user.getWard())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
