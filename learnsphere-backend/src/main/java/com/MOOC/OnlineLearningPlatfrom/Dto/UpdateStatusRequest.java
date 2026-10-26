package com.MOOC.OnlineLearningPlatfrom.Dto;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code PUT /api/admin/users/{id}/status}. */
public record UpdateStatusRequest(
        @NotBlank(message = "status is required")
        String status) {
}
