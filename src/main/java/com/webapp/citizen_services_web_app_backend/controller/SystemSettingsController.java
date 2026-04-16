package com.webapp.citizen_services_web_app_backend.controller;

import com.webapp.citizen_services_web_app_backend.entity.SystemSettings;
import com.webapp.citizen_services_web_app_backend.repository.SystemSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class SystemSettingsController {

    private SystemSettingsRepository settingsRepo;

    private static final List<String> CATEGORIES = new ArrayList<>(
            Arrays.asList("TRANSPORT", "WATER", "ELECTRICITY", "WASTE")
    );

//    public void SettingsController(SystemSettingsRepository settingsRepo) {
//        this.settingsRepo = settingsRepo;
//    }

    public SystemSettingsController(SystemSettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    // ── Helper: load or create the singleton settings row ──────────────────
    private SystemSettings loadOrCreate() {
        return settingsRepo.findById(1L).orElseGet(() -> {
            SystemSettings s = new SystemSettings();
            s.setId(1L);
            s.setAutoRoutingEnabled(false);
            s.setAdminEmailNotifications(false);
            return settingsRepo.save(s);
        });
    }

    // ── GET /api/admin/settings ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        SystemSettings s = loadOrCreate();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("autoRoutingEnabled", s.isAutoRoutingEnabled());
        result.put("adminEmailNotifications", s.isAdminEmailNotifications());
        // SLA fields — stored on the entity if you add them,
        // or returned as safe defaults here.
        result.put("slaOverdueDays",       getSlaOverdueDays(s));
        result.put("slaAutoEscalateDays",  getSlaAutoEscalateDays(s));
        return ResponseEntity.ok(result);
    }

    // ── PUT /api/admin/settings ─────────────────────────────────────────────
//    @PutMapping
//    public ResponseEntity<Map<String, Object>> saveSettings(@RequestBody Map<String, Object> body) {
//        SystemSettings s = loadOrCreate();
//
//        if (body.containsKey("autoRoutingEnabled"))
//            s.setAutoRoutingEnabled(Boolean.parseBoolean(body.get("autoRoutingEnabled").toString()));
//
//        if (body.containsKey("adminEmailNotifications"))
//            s.setAdminEmailNotifications(Boolean.parseBoolean(body.get("adminEmailNotifications").toString()));
//
//        // ── If you add slaOverdueDays / slaAutoEscalateDays fields to the
//        //    SystemSettings entity, uncomment these lines: ──
//        // if (body.containsKey("slaOverdueDays"))
//        //     s.setSlaOverdueDays(Integer.parseInt(body.get("slaOverdueDays").toString()));
//        // if (body.containsKey("slaAutoEscalateDays"))
//        //     s.setSlaAutoEscalateDays(Integer.parseInt(body.get("slaAutoEscalateDays").toString()));
//
//        settingsRepo.save(s);
//
//        // Return updated state
//        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("autoRoutingEnabled", s.isAutoRoutingEnabled());
//        result.put("adminEmailNotifications", s.isAdminEmailNotifications());
//        result.put("slaOverdueDays",      getSlaOverdueDays(s));
//        result.put("slaAutoEscalateDays", getSlaAutoEscalateDays(s));
//        return ResponseEntity.ok(result);
//    }

    // ── GET /api/admin/settings/categories ─────────────────────────────────
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(new ArrayList<>(CATEGORIES));
    }

    // ── POST /api/admin/settings/categories ────────────────────────────────
    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, String> body) {
        String cat = body.get("category");
        if (cat == null || cat.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));

        cat = cat.toUpperCase().trim();
        if (CATEGORIES.contains(cat))
            return ResponseEntity.badRequest().body(Map.of("error", "Category already exists"));

        CATEGORIES.add(cat);
        return ResponseEntity.ok(Map.of("category", cat));
    }

    // ── DELETE /api/admin/settings/categories/{name} ───────────────────────
    @DeleteMapping("/categories/{name}")
    public ResponseEntity<?> removeCategory(@PathVariable String name) {
        String cat = name.toUpperCase();
        // Protect the four built-in categories from deletion
        if (List.of("TRANSPORT","WATER","ELECTRICITY","WASTE").contains(cat))
            return ResponseEntity.badRequest().body(Map.of("error", "Built-in categories cannot be removed"));

        boolean removed = CATEGORIES.remove(cat);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    // ── SLA helpers (returns defaults until you add fields to the entity) ───
    private int getSlaOverdueDays(SystemSettings s) {
        // Replace with s.getSlaOverdueDays() once you add that field
        return 3;
    }
    private int getSlaAutoEscalateDays(SystemSettings s) {
        // Replace with s.getSlaAutoEscalateDays() once you add that field
        return 7;
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// OPTIONAL — Add these two fields to SystemSettings.java for full SLA persistence
// ─────────────────────────────────────────────────────────────────────────────
//
//   private int slaOverdueDays = 3;
//   private int slaAutoEscalateDays = 7;
//
//   // + getters / setters:
//   public int getSlaOverdueDays() { return slaOverdueDays; }
//   public void setSlaOverdueDays(int slaOverdueDays) { this.slaOverdueDays = slaOverdueDays; }
//   public int getSlaAutoEscalateDays() { return slaAutoEscalateDays; }
//   public void setSlaAutoEscalateDays(int v) { this.slaAutoEscalateDays = v; }
//
// Then uncomment the corresponding lines in SettingsController.java above.
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// INTEGRATION NOTES
// ─────────────────────────────────────────────────────────────────────────────
//
// 1. The ReportsTab already calls these endpoints from your existing controller:
//      GET /api/admin/overview/charts/daily          ← AdminDashboardController line ~68
//      GET /api/admin/overview/charts/by-category    ← AdminDashboardController line ~78
//      GET /api/admin/overview/charts/by-status      ← AdminDashboardController line ~88
//    No backend changes needed for the charts — they already exist.
//
// 2. For the SettingsTab, just drop SettingsController.java into your controller
//    package. Spring Boot will auto-wire it.
//
// 3. Category persistence: The static List<String> in SettingsController resets
//    when the server restarts. To persist categories, create a Category entity:
//
//    @Entity @Table(name = "complaint_categories")
//    public class ComplaintCategory {
//        @Id @GeneratedValue private Long id;
//        @Column(unique = true) private String name;
//        // getters/setters
//    }
//
//    Then replace the static list with a CategoryRepository.
//    This is optional — the static list works fine for most cases.
// ─────────────────────────────────────────────────────────────────────────────
