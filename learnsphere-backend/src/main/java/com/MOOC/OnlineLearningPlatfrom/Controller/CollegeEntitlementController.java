package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AdminGrantEntitlementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.AdminRevokeEntitlementRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementStatusDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.CollegeEntitlement;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CollegeEntitlementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CollegeEntitlementController {

    private final CollegeEntitlementService collegeEntitlementService;

    public CollegeEntitlementController(CollegeEntitlementService collegeEntitlementService) {
        this.collegeEntitlementService = collegeEntitlementService;
    }

    @GetMapping("/courses/{courseId}/entitlement")
    public ResponseEntity<CollegeEntitlementStatusDto> getCourseEntitlement(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : null;
        CollegeEntitlementStatusDto status = collegeEntitlementService.checkEntitlement(userId, courseId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(status);
    }

    @PostMapping("/admin/entitlements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollegeEntitlementResponseDto> grantAdminOverride(
            @RequestBody AdminGrantEntitlementRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : null;
        CollegeEntitlement entitlement = collegeEntitlementService.grantAdminOverride(
                request.getStudentId(), request.getCourseId(), adminId, request.getReason());
        return ResponseEntity.ok(CollegeEntitlementResponseDto.from(entitlement));
    }

    @DeleteMapping("/admin/entitlements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeEntitlement(
            @PathVariable Long id,
            @RequestBody(required = false) AdminRevokeEntitlementRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long adminId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : null;
        String reason = request != null ? request.getReason() : null;
        collegeEntitlementService.revokeEntitlement(id, adminId, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/entitlements/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CollegeEntitlementResponseDto>> getStudentEntitlements(@PathVariable Long studentId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(collegeEntitlementService.getStudentEntitlements(studentId));
    }

    @GetMapping("/admin/entitlements/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CollegeEntitlementResponseDto>> getCourseEntitlements(@PathVariable Long courseId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(collegeEntitlementService.getCourseEntitlements(courseId));
    }
}
