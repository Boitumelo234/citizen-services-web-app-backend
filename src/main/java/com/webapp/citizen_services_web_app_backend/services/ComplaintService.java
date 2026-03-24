package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintUpdateDTO;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.ComplaintUpdate;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
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

    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO dto, MultipartFile photo, Authentication authentication) {
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

        Complaint saved = complaintRepository.save(complaint);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileStorageService.storeFile(photo, saved.getId());
            saved.setPhotoUrl(photoUrl);
            saved = complaintRepository.save(saved);
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

    public void addComplaintUpdate(Long complaintId, String comment, String newLocation, MultipartFile photo, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        if (!complaint.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You do not own this complaint");
        }

        ComplaintUpdate update = new ComplaintUpdate();
        update.setComment(comment.trim());
        if (newLocation != null && !newLocation.trim().isEmpty()) {
            update.setNewLocation(newLocation.trim());
        }
        update.setUser(currentUser);
        update.setComplaint(complaint);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileStorageService.storeFile(photo, complaintId);
            update.setPhotoUrl(photoUrl);
        }

        complaint.getUpdates().add(update);
        complaintRepository.save(complaint);
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

        dto.setUpdates(complaint.getUpdates().stream()
                .map(this::mapToUpdateDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private ComplaintUpdateDTO mapToUpdateDTO(ComplaintUpdate update) {
        ComplaintUpdateDTO dto = new ComplaintUpdateDTO();
        dto.setId(update.getId());
        dto.setComment(update.getComment());
        dto.setNewLocation(update.getNewLocation());
        dto.setPhotoUrl(update.getPhotoUrl());
        dto.setCreatedAt(update.getCreatedAt());
        return dto;
    }

    // 1. Get all complaints for the Admin dashboard
    public List<ComplaintResponseDTO> getAllComplaintsForAdmin() {
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // 2. Update the status of a specific complaint
    public void updateComplaintStatus(Long id, String newStatus) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        complaint.setStatus(newStatus);
        complaintRepository.save(complaint);
    }

}