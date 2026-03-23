package com.webapp.citizen_services_web_app_backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000")
public class ComplaintController {

    @GetMapping
    public List<Map<String, Object>> getAllComplaints() {

        List<Map<String, Object>> complaints = new ArrayList<>();

        Map<String, Object> complaint1 = new HashMap<>();
        complaint1.put("id", 1);
        complaint1.put("title", "Water leakage");
        complaint1.put("department", "Water");
        complaint1.put("status", "Open");

        Map<String, Object> complaint2 = new HashMap<>();
        complaint2.put("id", 2);
        complaint2.put("title", "Pothole on Main Road");
        complaint2.put("department", "Roads");
        complaint2.put("status", "Resolved");

        complaints.add(complaint1);
        complaints.add(complaint2);

        return complaints;
    }
}