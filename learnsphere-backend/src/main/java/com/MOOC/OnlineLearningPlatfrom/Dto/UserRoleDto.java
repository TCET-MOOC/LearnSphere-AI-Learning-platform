package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;

/**
 * DTO for a role assignment. The legacy endpoints used to return the
 * {@link UserRole} entity, which serialised the whole nested user and role.
 */
public record UserRoleDto(Long id, Long userId, Long roleId, String roleName) {

    public static UserRoleDto from(UserRole userRole) {
        return new UserRoleDto(
                userRole.getUserRoleId(),
                userRole.getUser() != null ? userRole.getUser().getUserId() : null,
                userRole.getRole() != null ? userRole.getRole().getRoleId() : null,
                userRole.getRole() != null ? userRole.getRole().getName() : null);
    }
}
