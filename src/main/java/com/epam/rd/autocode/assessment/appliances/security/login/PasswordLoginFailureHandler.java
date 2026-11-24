package com.epam.rd.autocode.assessment.appliances.security.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Generic localized redirects without saving exceptions or credentials in a session.
@Component
public class PasswordLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failure)
            throws IOException {
        if (failure instanceof AuthenticationServiceException) {
            response.setHeader("Cache-Control", "no-store");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        response.sendRedirect(
                request.getContextPath()
                        + (failure instanceof LockedException ? "/login?locked" : "/login?error"));
    }
}
