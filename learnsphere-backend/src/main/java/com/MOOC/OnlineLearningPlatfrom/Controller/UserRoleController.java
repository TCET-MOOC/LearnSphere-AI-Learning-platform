package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CreateUserRoleRequest;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserRoleDto;
import com.MOOC.OnlineLearningPlatfrom.Service.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only role-assignment surface. It returns a DTO instead of the
 * {@code UserRole} entity and every write is audited by the service.
 */
@RestController
@RequestMapping("/api/user-roles")
@PreAuthorize("hasRole('ADMIN')")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public ResponseEntity<List<UserRoleDto>> getAllUserRoles() {
        return privateNoStore(userRoleService.getAllUserRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRoleDto> getUserRoleById(@PathVariable Long id) {
        return privateNoStore(userRoleService.getUserRoleById(id));
    }

    @PostMapping
    public ResponseEntity<UserRoleDto> createUserRole(@Valid @RequestBody CreateUserRoleRequest request) {
        return ResponseEntity.ok(userRoleService.assignRole(request.userId(), request.roleId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserRole(@PathVariable Long id) {
        userRoleService.deleteUserRole(id);
        return ResponseEntity.noContent().build();
    }

    private static <T> ResponseEntity<T> privateNoStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(body);
    }
}
