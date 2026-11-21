package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.web.MvcSecurityFailures;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcRouteClassifier;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Expired access may lead to a CSRF-protected renewal form, never to replaying an unsafe request. */
@RequiredArgsConstructor
public final class JwtBrowserFailures implements AuthenticationEntryPoint {
    private final AuthTokenCookies cookies;
    private final MvcSecurityFailures failures;
    private final MvcRouteClassifier routes;
    @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException, ServletException {
        if (failure instanceof AuthenticationServiceException) {
            response.setHeader("Cache-Control", "no-store");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE); return;
        }
        if (failure instanceof CredentialsExpiredException) {
            cookies.clear(request, response);
            failures.commence(request, response, failure);
            return; // Revoked credentials must not offer renewal using the same browser's old refresh cookie.
        }
        cookies.clearAccess(request, response);
        if (!routes.isUnmappedRead(request)) {
            try {
                if (cookies.readRefresh(request) != null) {
                    response.sendRedirect(request.getContextPath() + "/auth/renew"); return;
                }
            } catch (IllegalArgumentException malformed) { cookies.clear(request, response); }
        }
        failures.commence(request, response, failure);
    }
}
