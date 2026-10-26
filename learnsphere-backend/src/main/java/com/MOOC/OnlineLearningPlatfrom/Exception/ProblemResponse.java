package com.MOOC.OnlineLearningPlatfrom.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.List;

/**
 * RFC 9457 {@code application/problem+json} response body.
 *
 * <p>{@code type}, {@code title}, {@code status}, {@code detail} and
 * {@code instance} are the standard members. {@code code} and {@code traceId}
 * are the project's stable extensions: clients branch on {@code code}, and
 * support can join a report to the JSON logs through {@code traceId} (the same
 * value is returned in the {@code X-Trace-Id} header).</p>
 */
@Schema(name = "Problem", description = "RFC 9457 problem+json error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemResponse(
        @Schema(description = "Stable problem type URI",
                example = "https://learnsphere.ai/problems/not-found")
        URI type,
        @Schema(description = "HTTP reason phrase", example = "Not Found")
        String title,
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "Human-readable explanation for this occurrence")
        String detail,
        @Schema(description = "Request path that produced the problem")
        URI instance,
        @Schema(description = "Stable application error code", example = "NOT_FOUND")
        String code,
        @Schema(description = "Correlation id, also returned in the X-Trace-Id header")
        String traceId,
        @Schema(description = "Field failures for VALIDATION_FAILED")
        List<ValidationError> errors) {
}
