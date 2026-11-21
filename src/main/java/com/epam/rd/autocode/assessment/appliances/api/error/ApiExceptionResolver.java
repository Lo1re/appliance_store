package com.epam.rd.autocode.assessment.appliances.api.error;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/** Resolves only /api errors, including errors before a controller has been selected. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiExceptionResolver implements HandlerExceptionResolver {
    private final ApiExceptionMapper errors;
    private final ObjectMapper json;

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                          Object handler, Exception failure) {
        String root = request.getContextPath() + "/api";
        if (!request.getRequestURI().equals(root) && !request.getRequestURI().startsWith(root + "/")) return null;
        var error = errors.handle(failure, request);
        response.setStatus(error.getStatusCode().value());
        error.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        try { json.writeValue(response.getOutputStream(), error.getBody()); }
        catch (IOException unavailable) { log.error("API error response could not be written", SafeFailure.snapshot(unavailable)); }
        return new ModelAndView();
    }
}
