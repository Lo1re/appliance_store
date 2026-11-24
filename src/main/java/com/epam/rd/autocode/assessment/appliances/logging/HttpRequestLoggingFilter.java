package com.epam.rd.autocode.assessment.appliances.logging;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId =
                request.getAttribute(RequestLogContext.ATTRIBUTE) instanceof String existing
                        ? existing
                        : UUID.randomUUID().toString();
        request.setAttribute(RequestLogContext.ATTRIBUTE, requestId);
        response.setHeader(RequestLogContext.HEADER, requestId);
        String previous = MDC.get(RequestLogContext.MDC_KEY);
        MDC.put(RequestLogContext.MDC_KEY, requestId);
        boolean errorDispatch = request.getDispatcherType() == DispatcherType.ERROR;
        long started = System.nanoTime();
        boolean completed = false;
        try {
            if (!errorDispatch) {
                var metadata = HttpLogMetadata.capture(request);
                log.debug(
                        "HTTP request started (method={}, clientIp={}, requestBytes={},"
                                + " dispatcher={})",
                        metadata.method(),
                        metadata.clientIp(),
                        metadata.requestBytes(),
                        request.getDispatcherType());
                log.trace(
                        "HTTP diagnostic metadata (authorizationHeaderPresent={},"
                                + " cookieHeaderPresent={}, asyncStarted={})",
                        request.getHeader("Authorization") != null,
                        request.getHeader("Cookie") != null,
                        request.isAsyncStarted());
            }
            chain.doFilter(request, response);
            completed = true;
        } catch (IOException | ServletException | RuntimeException failure) {
            var metadata = HttpLogMetadata.capture(request);
            log.error(
                    "HTTP request failed (method={}, path={}, route={}, handler={}, clientIp={},"
                            + " type={})",
                    metadata.method(),
                    metadata.path(),
                    metadata.route(),
                    metadata.handler(),
                    metadata.clientIp(),
                    failure.getClass().getSimpleName(),
                    SafeFailure.snapshot(failure));
            throw failure;
        } finally {
            if (completed && !errorDispatch) {
                long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                var metadata = HttpLogMetadata.capture(request);
                int status = response.getStatus();
                Level level = status >= 500 ? Level.ERROR : status >= 400 ? Level.WARN : Level.INFO;
                String outcome =
                        status >= 500
                                ? "FAILED"
                                : status >= 400
                                        ? "REJECTED"
                                        : status >= 300 ? "REDIRECT" : "SUCCESS";
                log.atLevel(level)
                        .log(
                                "HTTP request completed (method={}, path={}, route={}, handler={},"
                                        + " status={}, outcome={}, durationMs={}, clientIp={},"
                                        + " requestBytes={})",
                                metadata.method(),
                                metadata.path(),
                                metadata.route(),
                                metadata.handler(),
                                status,
                                outcome,
                                elapsed,
                                metadata.clientIp(),
                                metadata.requestBytes());
            }
            if (previous == null) {
                MDC.remove(RequestLogContext.MDC_KEY);
            } else {
                MDC.put(RequestLogContext.MDC_KEY, previous);
            }
        }
    }
}
