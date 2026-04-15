package com.webapp.citizen_services_web_app_backend.services;

import com.webapp.citizen_services_web_app_backend.dto.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private static final DateTimeFormatter RECENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ==================== Original / Simple Methods ====================

    public Complaint submitComplaint(String title, String category, String description,
                                     String area, String priority, String citizenEmail) {

        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new RuntimeException("Citizen not found with email: " + citizenEmail));

        Complaint complaint = new Complaint();
        complaint.setTitle(title);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setArea(area);
        complaint.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");
        complaint.setStatus("PENDING");
        complaint.setCitizen(citizen);

        return complaintRepository.save(complaint);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }

    public Complaint updateStatus(Long id, String newStatus, String assignedToEmail) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        complaint.setStatus(newStatus.toUpperCase());

        if (assignedToEmail != null && !assignedToEmail.isBlank()) {
            User staff = userRepository.findByEmail(assignedToEmail)
                    .orElseThrow(() -> new RuntimeException("Staff not found with email: " + assignedToEmail));
            complaint.setAssignedTo(String.valueOf(staff));
        }

        return complaintRepository.save(complaint);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus("RESOLVED");
        long inProgress = complaintRepository.countByStatus("IN_PROGRESS");
        long pending = complaintRepository.countByStatus("PENDING");
        long assigned = complaintRepository.countByStatus("ASSIGNED");

        stats.put("totalComplaints", total);
        stats.put("resolvedComplaints", resolved);
        stats.put("inProgress", inProgress);
        stats.put("pendingComplaints", pending);
        stats.put("assignedComplaints", assigned);

        List<Object[]> statusData = complaintRepository.countByStatusGrouped();
        List<Map<String, Object>> statusDist = statusData.stream().map(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", row[0]);
            entry.put("count", row[1]);
            return entry;
        }).collect(Collectors.toList());

        stats.put("statusDistribution", statusDist);

        List<Complaint> recent = complaintRepository.findAll().stream()
                .sorted(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> recentList = recent.stream().map(c -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", c.getId());
            entry.put("title", c.getTitle());
            entry.put("category", c.getCategory());
            entry.put("area", c.getArea());
            entry.put("status", c.getStatus());
            entry.put("priority", c.getPriority());
            entry.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            return entry;
        }).collect(Collectors.toList());

        stats.put("recentComplaints", recentList);
        return stats;
    }

    // ==================== Main Branch Advanced Methods ====================

    public String generateReferenceNumber() {
        long count = complaintRepository.count() + 1;
        int year = LocalDateTime.now().getYear();
        return String.format("RLM-%d-%03d", year, count);
    }

    public List<Complaint> getAllComplaintsOrdered() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public Complaint updateStatusWithString(Long id, String newStatus, String assignedTo) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        complaint.setStatus(newStatus);
        if (assignedTo != null && !assignedTo.isBlank()) {
            complaint.setAssignedTo(assignedTo);
        }
        if ("Resolved".equalsIgnoreCase(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        return complaintRepository.save(complaint);
    }

    // Unified submitComplaint with DTO and photo
    public ComplaintResponseDTO submitComplaint(ComplaintRequestDTO dto, MultipartFile photo, Authentication authentication) {
        validateComplaintRequest(dto);
        User user = getCurrentUser(authentication.getName());

        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setReferenceNumber(generateReferenceNumber());
        complaint.setTitle(buildComplaintTitle(dto));
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
            String photoUrl = fileStorageService.storeFile(photo, saved.getId());
            saved.setPhotoUrl(photoUrl);
            saved = complaintRepository.save(saved);
        }

        return toResponseDto(saved);
    }

    public List<ComplaintResponseDTO> getMyComplaints(Authentication authentication) {
        User user = getCurrentUser(authentication.getName());
        return complaintRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
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
            String photoUrl = fileStorageService.storeFile(photo, complaintId);
            update.setPhotoUrl(photoUrl);
        }

        complaint.getUpdates().add(update);
        complaintRepository.save(complaint);
    }

    // ==================== Dashboard & Overview Helpers ====================

    public List<DashboardDTO.CategoryCount> getCategoryCounts(User user) {
        Map<String, Long> counts = complaintRepository.findByUser(user)
                .stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> DashboardDTO.CategoryCount.builder().name(entry.getKey()).count(entry.getValue()).build())
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());

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
                .collect(Collectors.toList());
    }

    public User getCurrentUserByEmail(String email) {
        return getCurrentUser(email);
    }

    // ==================== Private Helpers ====================

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
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

    private String buildComplaintTitle(ComplaintRequestDTO dto) {
        String category = dto.getCategory() == null ? "Complaint" : dto.getCategory().trim();
        String location = dto.getLocation() == null ? "" : dto.getLocation().trim();

        if (!location.isBlank()) {
            return category + " - " + location;
        }

        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        if (!description.isBlank()) {
            return description.length() <= 80 ? description : description.substring(0, 77) + "...";
        }

        return category;
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

        if (complaint.getUpdates() != null) {
            dto.setUpdates(complaint.getUpdates().stream()
                    .sorted(Comparator.comparing(ComplaintUpdate::getCreatedAt).reversed())
                    .map(this::toUpdateDto)
                    .collect(Collectors.toList()));
        }
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
}
