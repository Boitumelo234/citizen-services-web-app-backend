package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.DashboardDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintUpdateDTO;
import com.webapp.citizen_services_web_app_backend.dto.OverviewDTO;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.entity.ComplaintUpdate;
import com.webapp.citizen_services_web_app_backend.entity.User;
import com.webapp.citizen_services_web_app_backend.repository.ComplaintRepository;
import com.webapp.citizen_services_web_app_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private static final DateTimeFormatter RECENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO dto, MultipartFile photo, Authentication authentication) {
        validateComplaintRequest(dto);
        User user = getCurrentUser(authentication.getName());

        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setReferenceNumber(generateReferenceNumber());
        complaint.setCategory(dto.getCategory());
        complaint.setLocation(resolveLocation(dto));
        complaint.setDescription(dto.getDescription());
        complaint.setPriority(dto.getPriority() == null || dto.getPriority().isBlank() ? "medium" : dto.getPriority());
        complaint.setStatus("Pending");
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setSubmittedAt(complaint.getCreatedAt());

        Complaint saved = complaintRepository.save(complaint);

        if (photo != null && !photo.isEmpty()) {
            saved.setPhotoUrl(fileStorageService.storeFile(photo, saved.getId()));
            saved = complaintRepository.save(saved);
        }

        return toResponseDto(saved);
    }

    public List<ComplaintResponseDTO> getMyComplaints(Authentication authentication) {
        User user = getCurrentUser(authentication.getName());
        return complaintRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public void addComplaintUpdate(Long complaintId, String comment, String newLocation, MultipartFile photo, Authentication authentication) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        User currentUser = getCurrentUser(authentication.getName());
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        if (!complaint.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You do not own this complaint");
        }

        ComplaintUpdate update = new ComplaintUpdate();
        update.setComment(comment.trim());
        update.setNewLocation((newLocation == null || newLocation.isBlank()) ? null : newLocation.trim());
        update.setUser(currentUser);
        update.setComplaint(complaint);

        if (photo != null && !photo.isEmpty()) {
            update.setPhotoUrl(fileStorageService.storeFile(photo, complaintId));
        }

        complaint.getUpdates().add(update);
        complaintRepository.save(complaint);
    }

    public List<DashboardDTO.CategoryCount> getCategoryCounts(User user) {
        Map<String, Long> counts = complaintRepository.findByUser(user)
                .stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> DashboardDTO.CategoryCount.builder().name(entry.getKey()).count(entry.getValue()).build())
                .toList();
    }

    public List<DashboardDTO.RecentComplaint> getRecentComplaints(User user) {
        return complaintRepository.findTop5ByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(complaint -> DashboardDTO.RecentComplaint.builder()
                        .id(complaint.getReferenceNumber())
                        .title(summarizeDescription(complaint.getDescription()))
                        .category(complaint.getCategory())
                        .status(complaint.getStatus())
                        .date(complaint.getCreatedAt() == null ? "" : complaint.getCreatedAt().format(RECENT_DATE_FORMAT))
                        .build())
                .toList();
    }

    public long countComplaints(User user) {
        return complaintRepository.countByUser(user);
    }

    public long countResolvedThisMonth(User user) {
        return complaintRepository.countResolvedByUserSince(
                user,
                "Resolved",
                YearMonth.now().atDay(1).atStartOfDay()
        );
    }

    public long countResolved(User user) {
        return complaintRepository.countByUserAndStatusIgnoreCase(user, "Resolved");
    }

    public long countOpen(User user) {
        return complaintRepository.findByUser(user)
                .stream()
                .filter(complaint -> !isResolved(complaint))
                .count();
    }

    public double averageResolutionDays(User user) {
        List<Complaint> resolvedComplaints = complaintRepository.findByUser(user)
                .stream()
                .filter(this::hasResolutionWindow)
                .toList();

        if (resolvedComplaints.isEmpty()) {
            return 0;
        }

        double avgDays = resolvedComplaints.stream()
                .mapToLong(complaint -> java.time.Duration.between(complaint.getCreatedAt(), complaint.getResolvedAt()).toHours())
                .average()
                .orElse(0) / 24.0;

        return Math.round(avgDays * 10.0) / 10.0;
    }

    public List<OverviewDTO.MonthlyTrend> getMonthlyTrend(User user) {
        Map<YearMonth, Long> grouped = complaintRepository.findByUser(user)
                .stream()
                .filter(complaint -> complaint.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        complaint -> YearMonth.from(complaint.getCreatedAt()),
                        Collectors.counting()
                ));

        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(offset -> YearMonth.now().minusMonths(5L - offset))
                .map(month -> OverviewDTO.MonthlyTrend.builder()
                        .month(month.getMonth().name().substring(0, 3).toUpperCase(Locale.ROOT))
                        .count(grouped.getOrDefault(month, 0L))
                        .build())
                .toList();
    }

    public User getCurrentUserByEmail(String email) {
        return getCurrentUser(email);
    }

    private ComplaintResponseDTO toResponseDto(Complaint complaint) {
        ComplaintResponseDTO dto = new ComplaintResponseDTO();
        dto.setId(complaint.getId());
        dto.setReferenceNumber(complaint.getReferenceNumber());
        dto.setCategory(complaint.getCategory());
        dto.setLocation(complaint.getLocation());
        dto.setDescription(complaint.getDescription());
        dto.setStatus(complaint.getStatus());
        dto.setCreatedAt(complaint.getCreatedAt());
        dto.setPhotoUrl(complaint.getPhotoUrl());
        dto.setUpdates(
                complaint.getUpdates().stream()
                        .sorted(Comparator.comparing(ComplaintUpdate::getCreatedAt).reversed())
                        .map(this::toUpdateDto)
                        .toList()
        );
        return dto;
    }

    private ComplaintUpdateDTO toUpdateDto(ComplaintUpdate update) {
        ComplaintUpdateDTO dto = new ComplaintUpdateDTO();
        dto.setId(update.getId());
        dto.setComment(update.getComment());
        dto.setNewLocation(update.getNewLocation());
        dto.setPhotoUrl(update.getPhotoUrl());
        dto.setCreatedAt(update.getCreatedAt());
        return dto;
    }

    private User getCurrentUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private void validateComplaintRequest(ComplaintRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Complaint payload is required");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
    }

    private String generateReferenceNumber() {
        long count = complaintRepository.count() + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("RLM-%d-%04d", year, count);
    }

    private String resolveLocation(ComplaintRequestDTO dto) {
        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            return dto.getLocation().trim();
        }
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            return String.format(Locale.ENGLISH, "%.6f, %.6f", dto.getLatitude(), dto.getLongitude());
        }
        return "Location not specified";
    }

    private boolean isResolved(Complaint complaint) {
        return complaint.getStatus() != null && complaint.getStatus().equalsIgnoreCase("Resolved");
    }

    private boolean hasResolutionWindow(Complaint complaint) {
        return isResolved(complaint) && complaint.getCreatedAt() != null && complaint.getResolvedAt() != null;
    }

    private String summarizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Complaint update";
        }
        String trimmed = description.trim();
        return trimmed.length() <= 48 ? trimmed : trimmed.substring(0, 45) + "...";
    }
}
