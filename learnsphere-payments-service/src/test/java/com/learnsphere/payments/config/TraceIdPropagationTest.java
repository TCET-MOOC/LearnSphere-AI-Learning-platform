package com.learnsphere.payments.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TraceIdPropagationTest {

    @Test
    void shouldForwardTheTraceIdOnOutgoingCalls() {
        RestTemplate restTemplate = new AppConfig().restTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        MDC.put("traceId", "trace-out-123");
        try {
            server.expect(requestTo("http://backend.test/ping"))
                    .andExpect(header("X-Request-Id", "trace-out-123"))
                    .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

            assertThat(restTemplate.getForObject("http://backend.test/ping", String.class))
                    .isEqualTo("ok");
            server.verify();
        } finally {
            MDC.remove("traceId");
        }
    }
}
