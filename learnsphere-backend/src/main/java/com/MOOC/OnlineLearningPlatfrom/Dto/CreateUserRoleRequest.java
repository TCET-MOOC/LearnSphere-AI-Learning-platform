package com.MOOC.OnlineLearningPlatfrom.Dto;

import jakarta.validation.constraints.NotNull;

/** Body for {@code POST /api/user-roles}. */
public record CreateUserRoleRequest(
        @NotNull(message = "userId is required")
        Long userId,
        @NotNull(message = "roleId is required")
        Long roleId) {
}
