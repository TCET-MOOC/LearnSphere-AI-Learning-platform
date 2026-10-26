package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AssignRoleRequest;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserRoleDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.UserAccountService;
import com.MOOC.OnlineLearningPlatfrom.Service.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Self-service profile plus the admin read alias for a single user.
 *
 * <p>W1-11 retired the generic list/create/update/delete/status methods: an
 * account and its roles may only be read or changed through the admin-guarded
 * surface, and {@code /users/me} can change only the three profile fields a
 * user owns. Responses carry {@code private, no-store} (NFR-03).</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final UserRoleService userRoleService;

    public UserAccountController(UserAccountService userAccountService,
                                 UserRoleService userRoleService) {
        this.userAccountService = userAccountService;
        this.userRoleService = userRoleService;
    }

    /** GET /api/users/me - the authenticated user's own profile. */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        UserAccount user = currentUser(principal);
        return privateNoStore(UserResponseDto.from(user, roleNames(user.getUserId())));
    }

    /** Typed request body for PUT /api/users/me - only safe, self-editable fields. */
    public static class UpdateProfileRequest {
        private String fullName;
        private String bio;
        private String avatarUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    /** PUT /api/users/me - updates only the safe, self-editable fields. */
    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserAccount user = currentUser(principal);
        UserAccount updated = userAccountService.updateOwnProfile(
                user.getUserId(),
                request.getFullName(),
                request.getBio(),
                request.getAvatarUrl());
        return privateNoStore(UserResponseDto.from(updated, roleNames(updated.getUserId())));
    }

    /**
     * Admin read alias. The generic entity response was retired; this returns
     * the same password-free profile DTO as {@code /users/me}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserAccount user = userAccountService.getUserById(id);
        return privateNoStore(UserResponseDto.from(user, roleNames(id)));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRoleDto>> getUserRoles(@PathVariable Long id) {
        return privateNoStore(userRoleService.getRolesForUser(id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserRoleDto> assignRole(@PathVariable Long id,
                                                  @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userRoleService.assignRole(id, request.roleId()));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        userRoleService.removeRoleFromUser(id, roleId);
        return ResponseEntity.noContent().build();
    }

    private UserAccount currentUser(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return userAccountService.getUserById(principal.getUser().getUserId());
    }

    private List<String> roleNames(Long userId) {
        return userRoleService.getRolesForUser(userId).stream()
                .map(UserRoleDto::roleName)
                .toList();
    }

    private static <T> ResponseEntity<T> privateNoStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
