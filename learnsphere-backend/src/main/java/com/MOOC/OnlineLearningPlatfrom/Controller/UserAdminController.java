package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.PageResponse;
import com.MOOC.OnlineLearningPlatfrom.Dto.TopTeacherDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UpdateStatusRequest;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserActivitySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDetailDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserCountsDto;
import com.MOOC.OnlineLearningPlatfrom.Service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin user-management endpoints: listing/filtering users, flagging/blacklisting,
 * and real activity stats derived from UserAccount.lastActiveAt.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /** Paginated since W7-12 ({@code PageResponse}, zero-based, ordered by id). */
    @GetMapping
    public ResponseEntity<PageResponse<UserAdminDto>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return privateNoStore(userAdminService.getUsers(role, status, search, pageable));
    }

    /** Tab counts over every account (W7-12). */
    @GetMapping("/counts")
    public ResponseEntity<UserCountsDto> getCounts() {
        return privateNoStore(userAdminService.getCounts());
    }

    /** Inspect: account, roles, recent payments and audit history (W7-12, audit D5). */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserAdminDetailDto> getUserDetail(@PathVariable Long id) {
        return privateNoStore(userAdminService.getUserDetail(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserAdminDto> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userAdminService.updateStatus(id, request.status()));
    }

    @GetMapping("/activity-summary")
    public ResponseEntity<UserActivitySummaryDto> getActivitySummary() {
        return privateNoStore(userAdminService.getActivitySummary());
    }

    @GetMapping("/top-teachers")
    public ResponseEntity<List<TopTeacherDto>> getTopTeachers(@RequestParam(defaultValue = "3") int limit) {
        return privateNoStore(userAdminService.getTopTeachers(limit));
    }

    private static <T> ResponseEntity<T> privateNoStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
