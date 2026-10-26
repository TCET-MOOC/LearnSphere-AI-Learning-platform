package com.MOOC.OnlineLearningPlatfrom.Config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class TraceIdLoggingTest {

    @Test
    void shouldIncludeTraceIdInStructuredJsonLogs(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("X-Request-Id", "trace-log-123");

        new TraceIdFilter().doFilter(request, new MockHttpServletResponse(), (req, res) ->
                LoggerFactory.getLogger("trace-test").info("structured-log-probe"));

        assertThat(output).contains("\"traceId\":\"trace-log-123\"");
    }
}
