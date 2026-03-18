package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.dto.ComplaintRequestDTO;
import com.webapp.citizen_services_web_app_backend.dto.ComplaintResponseDTO;
import com.webapp.citizen_services_web_app_backend.entity.Complaint;
import com.webapp.citizen_services_web_app_backend.services.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // ── Existing endpoints ──
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submitComplaint(
            @RequestPart("data") @Valid ComplaintRequestDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        ComplaintResponseDTO saved = complaintService.submitComplaint(dto, photo, authentication);

        return ResponseEntity.ok(Map.of(
                "message", "Complaint submitted successfully",
                "referenceNumber", saved.getReferenceNumber(),
                "status", saved.getStatus(),
                "data", saved
        ));
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaints(Authentication authentication) {
        List<ComplaintResponseDTO> complaints = complaintService.getMyComplaints(authentication);
        return ResponseEntity.ok(complaints);
    }

    // ── NEW ENDPOINT: Add update/comment to a complaint ──
    @PostMapping(value = "/{id}/updates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> addUpdate(
            @PathVariable("id") Long complaintId,
            @RequestParam("comment") String comment,
            @RequestParam(value = "newLocation", required = false) String newLocation,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {

        if (comment == null || comment.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comment cannot be empty"));
        }

        try {
            complaintService.addComplaintUpdate(complaintId, comment, newLocation, photo, authentication);
            return ResponseEntity.ok(Map.of("message", "Update added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add update"));
        }
    }
}