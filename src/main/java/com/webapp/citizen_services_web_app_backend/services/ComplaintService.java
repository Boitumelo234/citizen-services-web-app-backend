package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import com.webapp.citizen_services_web_app_backend.services.FileStorageService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            FileStorageService fileStorageService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO dto,
                                                MultipartFile photo,
                                                Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setCategory(dto.getCategory());
        complaint.setLocation(dto.getLocation());
        complaint.setDescription(dto.getDescription());
        complaint.setStatus("Pending");
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setSubmittedAt(LocalDateTime.now());

        // Save first to generate ID
        Complaint saved = complaintRepository.save(complaint);

        // Handle file upload if present
        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = fileStorageService.storeFile(photo, saved.getId());
            saved.setPhotoUrl(photoUrl);
            saved = complaintRepository.save(saved); // update with photo URL
        }

        return mapToResponseDTO(saved);
    }

    public List<ComplaintResponseDTO> getMyComplaints(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<Complaint> complaints = complaintRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return complaints.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private ComplaintResponseDTO mapToResponseDTO(Complaint complaint) {
        ComplaintResponseDTO dto = new ComplaintResponseDTO();
        dto.setId(complaint.getId());
        dto.setReferenceNumber(complaint.getReferenceNumber());
        dto.setCategory(complaint.getCategory());
        dto.setLocation(complaint.getLocation());
        dto.setDescription(complaint.getDescription());
        dto.setStatus(complaint.getStatus());
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setPhotoUrl(complaint.getPhotoUrl());
        return dto;
    }

    // In ComplaintService.java
    public void addComplaintUpdate(Long complaintId, String comment, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        // Security check: only owner can add update
        if (!complaint.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You do not own this complaint");
        }

        // For now: just print / log (later you'll save it properly)
        System.out.println("Update added to complaint " + complaintId + " by " + email + ": " + comment);

        // ── Future real implementation ──
        // ComplaintUpdate update = new ComplaintUpdate();
        // update.setComment(comment);
        // update.setCreatedAt(LocalDateTime.now());
        // update.setUser(currentUser);
        // update.setComplaint(complaint);
        // complaint.getUpdates().add(update);
        // complaintRepository.save(complaint);
    }
}