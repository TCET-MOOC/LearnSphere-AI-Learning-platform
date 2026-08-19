package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.TopTeacherDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserActivitySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Service.UserAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin user-management endpoints: listing/filtering users, flagging/blacklisting,
 * and real activity stats derived from UserAccount.lastActiveAt.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserAdminDto>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userAdminService.getUsers(role, status, search));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserAdminDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        return ResponseEntity.ok(userAdminService.updateStatus(id, status));
    }

    @GetMapping("/activity-summary")
    public ResponseEntity<UserActivitySummaryDto> getActivitySummary() {
        return ResponseEntity.ok(userAdminService.getActivitySummary());
    }

    @GetMapping("/top-teachers")
    public ResponseEntity<List<TopTeacherDto>> getTopTeachers(@RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(userAdminService.getTopTeachers(limit));
    }
}
