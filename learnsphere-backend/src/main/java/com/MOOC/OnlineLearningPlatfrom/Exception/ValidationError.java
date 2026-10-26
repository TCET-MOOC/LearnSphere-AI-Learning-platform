package com.MOOC.OnlineLearningPlatfrom.Exception;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One field-level failure inside a {@link ProblemResponse}.
 *
 * @param field   the request field or parameter name
 * @param message a human-readable reason; never a stack trace
 */
@Schema(name = "ValidationError", description = "A single field validation failure")
public record ValidationError(String field, String message) {
}
