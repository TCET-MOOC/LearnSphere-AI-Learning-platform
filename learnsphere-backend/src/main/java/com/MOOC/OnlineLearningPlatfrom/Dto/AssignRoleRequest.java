package com.MOOC.OnlineLearningPlatfrom.Dto;

import jakarta.validation.constraints.NotNull;

/** Body for {@code POST /api/users/{id}/roles}. */
public record AssignRoleRequest(
        @NotNull(message = "roleId is required")
        Long roleId) {
}
