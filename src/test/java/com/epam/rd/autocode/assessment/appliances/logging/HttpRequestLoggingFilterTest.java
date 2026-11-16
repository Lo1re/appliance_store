package com.epam.rd.autocode.assessment.appliances.logging;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class HttpRequestLoggingFilterTest {
    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void correlatesMetadataAndRestoresThePreviousContextWithoutLoggingCredentials(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/orders/123");
        request.setQueryString("token=private-query-token");
        request.setContent("password=private-password".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer private-bearer-token");
        request.addHeader(RequestLogContext.HEADER, "untrusted-client-id");
        var response = new MockHttpServletResponse();
        MDC.put(RequestLogContext.MDC_KEY, "parent-context");
        var inside = new AtomicReference<String>();
        filter.doFilter(request, response, (incoming, outgoing) -> {
            inside.set(MDC.get(RequestLogContext.MDC_KEY));
            incoming.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/orders/{id}");
            ((jakarta.servlet.http.HttpServletResponse) outgoing).setStatus(201);
        });
        assertThat(response.getHeader(RequestLogContext.HEADER))
                .isEqualTo(inside.get()).isNotEqualTo("untrusted-client-id")
                .matches("[0-9a-f-]{36}");
        assertThat(MDC.get(RequestLogContext.MDC_KEY)).isEqualTo("parent-context");
        assertThat(output.getOut()).contains("route=/orders/{id}", "status=201")
                .doesNotContain("private-password", "private-query-token", "private-bearer-token", "/orders/123");
    }

    @Test
    void rethrowsOriginalFailureButLogsOnlySafeFailureDetails(CapturedOutput output) {
        var request = new MockHttpServletRequest("POST", "/orders/add");
        var response = new MockHttpServletResponse();
        var failure = new ServletException("private-password-marker", new IllegalStateException("private-token-marker"));
        assertThatThrownBy(() -> filter.doFilter(request, response, (incoming, outgoing) -> {
            throw failure;
        })).isSameAs(failure);
        assertThat(MDC.get(RequestLogContext.MDC_KEY)).isNull();
        assertThat(output.getOut()).contains("HTTP request failed", "ServletException", "IllegalStateException")
                .doesNotContain("private-password-marker", "private-token-marker", "HTTP request completed");
    }

    @Test
    void logsUnknownPathsAndSelectsSeverityFromTheResponseStatus(CapturedOutput output) throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            for (int status : new int[] {200, 302, 404, 500}) {
                var request = new MockHttpServletRequest("GET", "/appliances1");
                request.setQueryString("token=private-query-value");
                filter.doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) ->
                        ((jakarta.servlet.http.HttpServletResponse) outgoing).setStatus(status));
            }
            assertThat(appender.list.stream().filter(event -> event.getFormattedMessage()
                    .startsWith("HTTP request completed")).map(ch.qos.logback.classic.spi.ILoggingEvent::getLevel).toList())
                    .containsExactly(ch.qos.logback.classic.Level.INFO, ch.qos.logback.classic.Level.INFO,
                            ch.qos.logback.classic.Level.WARN, ch.qos.logback.classic.Level.ERROR);
            assertThat(output.getOut()).contains("path=/appliances1", "outcome=REJECTED", "outcome=FAILED")
                    .doesNotContain("private-query-value");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void errorDispatchReusesTheRequestIdWithoutDuplicatingTheRequestLog(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/error");
        request.setDispatcherType(DispatcherType.ERROR);
        String id = "e6c73e70-cf38-4be0-a877-105483875fe7";
        request.setAttribute(RequestLogContext.ATTRIBUTE, id);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) ->
                assertThat(MDC.get(RequestLogContext.MDC_KEY)).isEqualTo(id));
        assertThat(response.getHeader(RequestLogContext.HEADER)).isEqualTo(id);
        assertThat(output.getOut()).doesNotContain("HTTP request completed");
        assertThat(MDC.get(RequestLogContext.MDC_KEY)).isNull();
    }
}
