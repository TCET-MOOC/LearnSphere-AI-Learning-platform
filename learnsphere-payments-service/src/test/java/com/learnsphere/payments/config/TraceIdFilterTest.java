package com.learnsphere.payments.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void shouldUseIncomingTraceIdAndClearMdcAfterTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("X-Request-Id", "trace-abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                traceIdDuringChain.set(MDC.get("traceId")));

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-abc-123");
        assertThat(traceIdDuringChain).hasValue("trace-abc-123");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void shouldGenerateATraceIdWhenTheHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, (req, res) -> {
        });

        assertThat(response.getHeader("X-Trace-Id"))
                .isNotBlank()
                .matches("[0-9a-f-]{36}");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void shouldRejectAnUnsafeIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("X-Request-Id", "bad value\nforged-log-line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader("X-Trace-Id"))
                .isNotEqualTo("bad value\nforged-log-line")
                .matches("[0-9a-f-]{36}");
    }
}
