package com.epam.rd.autocode.assessment.appliances.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ApiSecurityFailures implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ApiProblems problems;
    private final ObjectMapper json;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failure)
            throws IOException {
        if (failure instanceof AuthenticationServiceException) {
            write(request, response, HttpStatus.SERVICE_UNAVAILABLE);
            return;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException failure)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        json.writeValue(
                response.getOutputStream(),
                problems.create(
                        status,
                        "http." + status.value(),
                        "error." + status.value() + ".message",
                        request));
    }
}
