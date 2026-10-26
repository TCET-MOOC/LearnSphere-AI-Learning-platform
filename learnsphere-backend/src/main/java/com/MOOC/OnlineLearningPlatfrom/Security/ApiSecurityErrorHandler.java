package com.MOOC.OnlineLearningPlatfrom.Security;

import com.MOOC.OnlineLearningPlatfrom.Exception.ApiErrorCode;
import com.MOOC.OnlineLearningPlatfrom.Exception.ProblemResponse;
import com.MOOC.OnlineLearningPlatfrom.Exception.ProblemResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Emits the same RFC 9457 body as the controller advice for failures raised by
 * the security filter chain, which never reaches {@code @RestControllerAdvice}.
 */
@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String UNAUTHENTICATED_DETAIL = "Authentication is required to access this resource";
    static final String ACCESS_DENIED_DETAIL = "You do not have permission to perform this action";

    private final ProblemResponseFactory problems;
    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(ProblemResponseFactory problems, ObjectMapper objectMapper) {
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, ApiErrorCode.UNAUTHORIZED, UNAUTHENTICATED_DETAIL);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, ApiErrorCode.FORBIDDEN, ACCESS_DENIED_DETAIL);
    }

    private void write(HttpServletRequest request,
                       HttpServletResponse response,
                       ApiErrorCode code,
                       String detail) throws IOException {
        ProblemResponse body = problems.create(code.status(), code, detail, request);
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
