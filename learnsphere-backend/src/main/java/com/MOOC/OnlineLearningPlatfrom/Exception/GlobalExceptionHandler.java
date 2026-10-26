package com.MOOC.OnlineLearningPlatfrom.Exception;

import com.MOOC.OnlineLearningPlatfrom.Config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps every controller exception onto the RFC 9457 contract.
 *
 * <p>The contract is {@code application/problem+json} with {@code type},
 * {@code title}, {@code status}, {@code detail}, {@code instance}, a stable
 * {@code code} and the request {@code traceId}. Unknown exceptions never leak
 * their message or stack trace; they become {@code INTERNAL_ERROR} and are
 * logged with the correlation id.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemResponseFactory problems;

    public GlobalExceptionHandler(ProblemResponseFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleNotFound(ResourceNotFoundException ex,
                                                          HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemResponse> handleBadRequest(BadRequestException ex,
                                                            HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemResponse> handleBadCredentials(BadCredentialsException ex,
                                                                HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_CREDENTIALS,
                "Invalid email or password", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemResponse> handleAuthentication(AuthenticationException ex,
                                                                HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED,
                "Authentication failed", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemResponse> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN,
                "You do not have permission to perform this action", request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemResponse> handleConflict(ConflictException ex,
                                                          HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.add(new ValidationError(fieldError.getField(), messageOrDefault(fieldError.getDefaultMessage()))));
        ex.getBindingResult().getGlobalErrors().forEach(globalError ->
                errors.add(new ValidationError(globalError.getObjectName(), messageOrDefault(globalError.getDefaultMessage()))));
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                "Validation failed", request, oneErrorPerField(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                     HttpServletRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        lastPathSegment(violation.getPropertyPath().toString()),
                        messageOrDefault(violation.getMessage())))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                "Validation failed", request, oneErrorPerField(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST,
                "The request body could not be read", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_PARAMETER,
                "Parameter '" + ex.getName() + "' has an invalid value", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                  HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_PARAMETER,
                "Required parameter '" + ex.getParameterName() + "' is missing", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                    HttpServletRequest request) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ApiErrorCode.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                       HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                                                HttpServletRequest request) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, ApiErrorCode.PAYLOAD_TOO_LARGE,
                "The uploaded file is too large", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                               HttpServletRequest request) {
        log.debug("Data integrity conflict [traceId={}]", currentTraceId(), ex);
        return respond(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT,
                "The request conflicts with the current state of the resource", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemResponse> handleNoResource(NoResourceFoundException ex,
                                                            HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND,
                "No resource found for this request", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemResponse> handleResponseStatus(ResponseStatusException ex,
                                                                HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String detail = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return respond(status, ApiErrorCode.forStatus(status), detail, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnexpected(Exception ex,
                                                            HttpServletRequest request) {
        log.error("Unhandled exception [traceId={}]", currentTraceId(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ProblemResponse> respond(HttpStatus status,
                                                    ApiErrorCode code,
                                                    String detail,
                                                    HttpServletRequest request) {
        return respond(status, code, detail, request, null);
    }

    private ResponseEntity<ProblemResponse> respond(HttpStatus status,
                                                    ApiErrorCode code,
                                                    String detail,
                                                    HttpServletRequest request,
                                                    List<ValidationError> errors) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problems.create(status, code, detail, request, errors));
    }

    /**
     * A field can violate several constraints at once. Clients render one
     * message per field, so keep the first message after a deterministic sort.
     */
    private static List<ValidationError> oneErrorPerField(List<ValidationError> errors) {
        Map<String, ValidationError> byField = new LinkedHashMap<>();
        errors.stream()
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
                .forEach(error -> byField.putIfAbsent(error.field(), error));
        return List.copyOf(byField.values());
    }

    private static String messageOrDefault(String message) {
        return message != null && !message.isBlank() ? message : "Invalid value";
    }

    private static String lastPathSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private static String currentTraceId() {
        return MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
    }
}
