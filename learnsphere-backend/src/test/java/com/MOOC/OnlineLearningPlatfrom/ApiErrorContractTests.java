package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the RFC 9457 contract from {@code Docs/PLAN.md} 6.2.
 *
 * <p>Every error a client can receive must be {@code application/problem+json}
 * with a stable {@code code} and the correlation {@code traceId}. This covers
 * controller errors, Bean Validation failures, malformed JSON, and errors
 * raised by the security filter chain before a controller runs.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiErrorContractTests.TestErrorController.class)
class ApiErrorContractTests extends PostgresIntegrationTest {

    private static final String TRACE_ID = "p0-09-contract";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProblemJsonForAMissingResource() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", 999_999L).header("X-Request-Id", TRACE_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Trace-Id", TRACE_ID))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://learnsphere.ai/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Course not found with id: 999999"))
                .andExpect(jsonPath("$.instance").value("/api/courses/999999"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void shouldReturnStableValidationCodesAndFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://learnsphere.ai/problems/validation-failed"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[1].field").value("password"))
                .andExpect(jsonPath("$.errors[1].message").isNotEmpty());
    }

    @Test
    void shouldReturnAStableCodeForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("The request body could not be read"));
    }

    @Test
    void shouldReturnProblemJsonWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/enrollments/me").header("X-Request-Id", TRACE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.instance").value("/api/enrollments/me"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void shouldReturnProblemJsonWhenAccessIsDenied() throws Exception {
        mockMvc.perform(get("/api/teacher/courses")
                        .with(user("p0-09-student").roles("STUDENT"))
                        .header("X-Request-Id", TRACE_ID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void shouldReturnProblemJsonForAnUnsupportedMethod() throws Exception {
        mockMvc.perform(delete("/api/courses")
                        .with(user("p0-09-student").roles("STUDENT")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.detail").value("HTTP method DELETE is not supported for this endpoint"));
    }

    @Test
    void shouldReturnProblemJsonForAnUnknownPath() throws Exception {
        mockMvc.perform(get("/api/p0-09-missing")
                        .with(user("p0-09-student").roles("STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("No resource found for this request"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldHideUnexpectedExceptionDetailsBehindAnInternalErrorCode(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/p0-09/boom")
                        .with(user("p0-09-student").roles("STUDENT"))
                        .header("X-Request-Id", TRACE_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(content().string(not(containsString("boom-secret"))));

        assertThat(output.getOut())
                .as("the server log keeps the correlation id and the stack trace for support")
                .contains("Unhandled exception [traceId=" + TRACE_ID + "]")
                .contains("IllegalStateException")
                .contains("boom-secret");
    }

    @RestController
    static class TestErrorController {

        @GetMapping("/test/p0-09/boom")
        void boom() {
            throw new IllegalStateException("boom-secret");
        }
    }
}
