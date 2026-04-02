package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintUpdateDTO;
import com.webapp.citizen_services_web_app_backend.dto.DepartmentPerformanceResponse;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.ComplaintUpdate;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
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

    public String generateReferenceNumber() {
        long count = complaintRepository.count() + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("RLM-%d-%03d", year, count);
    }

    public Complaint submitComplaint(String category, String description,
                                     String area, String priority,
                                     String citizenEmail) {
        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Citizen not found with email: " + citizenEmail));

        Complaint complaint = new Complaint();
        complaint.setReferenceNumber(generateReferenceNumber());
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setLocation(area);
        complaint.setPriority(priority != null ? priority : "Medium");
        complaint.setStatus("New");
        complaint.setUser(citizen);
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    public Complaint updateStatus(Long id, String newStatus, String assignedTo) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Complaint not found with id: " + id));

        complaint.setStatus(newStatus);
        if (assignedTo != null && !assignedTo.isBlank()) {
            complaint.setAssignedTo(assignedTo);
        }
        if ("Resolved".equalsIgnoreCase(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        return complaintRepository.save(complaint);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus("Resolved");
        long inProgress = complaintRepository.countByStatus("In Progress");
        long newCount = complaintRepository.countByStatus("New");
        long open = newCount + inProgress;

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);

        long newThisMonth = complaintRepository.countNewThisMonth(startOfMonth);
        long resolvedThisMonth = complaintRepository.countResolvedThisMonth(startOfMonth);
        long newThisWeek = complaintRepository.countComplaintsThisWeek(weekStart);

        Double avgHours = complaintRepository.getAvgResolutionTimeHours();
        double avgDays = (avgHours != null)
                ? Math.round((avgHours / 24.0) * 10.0) / 10.0
                : 0.0;

        stats.put("totalComplaints", total);
        stats.put("openComplaints", open);
        stats.put("resolvedComplaints", resolved);
        stats.put("pendingComplaints", newCount);
        stats.put("newThisMonth", newThisMonth);
        stats.put("resolvedThisMonth", resolvedThisMonth);
        stats.put("avgResolutionDays", avgDays);
        stats.put("newThisWeek", newThisWeek);

        return stats;
    }

    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO dto, MultipartFile photo, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setCategory(dto.getCategory());
        complaint.setLocation(dto.getLocation() != null ? dto.getLocation() :
                (dto.getLatitude() != null && dto.getLongitude() != null ?
                        String.format("%.6f, %.6f", dto.getLatitude(), dto.getLongitude()) : "Location not specified"));
        complaint.setDescription(dto.getDescription());
        complaint.setStatus("Pending");
        complaint.setPriority(dto.getPriority() != null ? dto.getPriority() : "medium");
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setSubmittedAt(LocalDateTime.now());

        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Complaint> complaints = complaintRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return complaints.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public void addComplaintUpdate(Long complaintId, String comment, String newLocation, MultipartFile photo, Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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

        if (complaint.getUpdates() != null) {
            dto.setUpdates(complaint.getUpdates().stream()
                    .map(this::mapToUpdateDTO)
                    .collect(Collectors.toList()));
        }

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
}