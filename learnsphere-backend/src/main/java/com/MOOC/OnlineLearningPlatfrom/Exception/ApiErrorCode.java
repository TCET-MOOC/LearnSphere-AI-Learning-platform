package com.MOOC.OnlineLearningPlatfrom.Exception;

import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Locale;

/**
 * Stable application error codes carried in the RFC 9457 {@code code} member.
 *
 * <p>Clients branch on these values, never on the human-readable
 * {@code detail}. Renaming a constant is a breaking API change; add a new code
 * instead. Each code owns both its HTTP status and its problem {@code type}
 * URI so the two cannot drift.</p>
 */
public enum ApiErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private static final String TYPE_BASE = "https://learnsphere.ai/problems/";

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public URI type() {
        return URI.create(TYPE_BASE + name().toLowerCase(Locale.ROOT).replace('_', '-'));
    }

    /**
     * Best-fit code for a status raised outside the typed handlers, such as a
     * {@code ResponseStatusException}. Specific handlers should still be
     * preferred so clients receive the most precise code.
     */
    public static ApiErrorCode forStatus(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 500 -> INTERNAL_ERROR;
            default -> status.is4xxClientError() ? BAD_REQUEST : INTERNAL_ERROR;
        };
    }
}
