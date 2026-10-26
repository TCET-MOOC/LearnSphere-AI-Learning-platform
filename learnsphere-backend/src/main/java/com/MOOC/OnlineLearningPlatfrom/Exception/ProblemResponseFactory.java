package com.MOOC.OnlineLearningPlatfrom.Exception;

import com.MOOC.OnlineLearningPlatfrom.Config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Builds the one error body shape used by controllers and the security chain.
 */
@Component
public class ProblemResponseFactory {

    public ProblemResponse create(HttpStatus status,
                                  ApiErrorCode code,
                                  String detail,
                                  HttpServletRequest request) {
        return create(status, code, detail, request, null);
    }

    public ProblemResponse create(HttpStatus status,
                                  ApiErrorCode code,
                                  String detail,
                                  HttpServletRequest request,
                                  List<ValidationError> errors) {
        return new ProblemResponse(
                code.type(),
                status.getReasonPhrase(),
                status.value(),
                detail,
                URI.create(request.getRequestURI()),
                code.name(),
                traceId(request),
                errors);
    }

    /**
     * Prefers the filter's MDC value so the body, response header and JSON log
     * lines all carry the same correlation id. The header fallbacks keep the
     * field populated if a problem is built outside the normal filter chain.
     */
    private String traceId(HttpServletRequest request) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        traceId = request.getHeader(TraceIdFilter.REQUEST_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        traceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return UUID.randomUUID().toString();
    }
}
