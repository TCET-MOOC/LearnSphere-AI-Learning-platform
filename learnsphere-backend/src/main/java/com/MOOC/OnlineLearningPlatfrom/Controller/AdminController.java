package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AffiliationRequestResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AuditLogResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PageResponse;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;
import com.MOOC.OnlineLearningPlatfrom.Service.AdminService;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    // Canonical path is /api/admin/audit (W1-03, api-catalogue); /audit-logs is a W6-10 alias.
    @GetMapping({"/audit", "/audit-logs"})
    public ResponseEntity<PageResponse<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String action,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AuditLogResponseDto> response =
                auditService.getAuditLogs(actorId, resourceType, resourceId, action, pageable);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(response);
    }

    @GetMapping("/colleges")
    public ResponseEntity<List<CollegeResponseDto>> getColleges() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(adminService.getAllColleges());
    }

    @PostMapping("/colleges")
    public ResponseEntity<CollegeResponseDto> createCollege(@RequestBody College college) {
        return ResponseEntity.ok(adminService.createCollege(college));
    }

    @PutMapping({"/colleges/{collegeId}/verify", "/colleges/{collegeId}/verification"})
    public ResponseEntity<Map<String, Boolean>> verifyCollege(@PathVariable Long collegeId) {
        adminService.verifyCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/colleges/{collegeId}/verify")
    public ResponseEntity<Map<String, Boolean>> verifyCollegePost(@PathVariable Long collegeId) {
        adminService.verifyCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping({"/colleges/{collegeId}", "/colleges/{collegeId}/reject"})
    public ResponseEntity<Map<String, Boolean>> rejectCollege(@PathVariable Long collegeId) {
        adminService.rejectCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/colleges/{collegeId}/reject")
    public ResponseEntity<Map<String, Boolean>> rejectCollegePost(@PathVariable Long collegeId) {
        adminService.rejectCollege(collegeId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping({"/colleges/affiliations", "/affiliations"})
    public ResponseEntity<List<AffiliationRequestResponseDto>> getAffiliationRequests() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(adminService.getAffiliationRequests());
    }

    @PutMapping({"/colleges/affiliations/{requestId}/approve", "/affiliations/{requestId}/approve"})
    public ResponseEntity<Map<String, Boolean>> approveAffiliation(@PathVariable Long requestId) {
        adminService.approveAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping({"/colleges/affiliations/{requestId}/approve", "/affiliations/{requestId}/approve"})
    public ResponseEntity<Map<String, Boolean>> approveAffiliationPost(@PathVariable Long requestId) {
        adminService.approveAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping({"/colleges/affiliations/{requestId}/reject", "/affiliations/{requestId}/reject"})
    public ResponseEntity<Map<String, Boolean>> rejectAffiliation(@PathVariable Long requestId) {
        adminService.rejectAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping({"/colleges/affiliations/{requestId}/reject", "/affiliations/{requestId}/reject"})
    public ResponseEntity<Map<String, Boolean>> rejectAffiliationPost(@PathVariable Long requestId) {
        adminService.rejectAffiliation(requestId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
