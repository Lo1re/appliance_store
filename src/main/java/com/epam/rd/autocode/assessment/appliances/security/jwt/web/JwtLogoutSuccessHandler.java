package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

// HTTP presentation only: do not claim server revocation succeeded when storage failed.
public final class JwtLogoutSuccessHandler implements LogoutSuccessHandler {
    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        response.setHeader("Cache-Control", "no-store");
        if (Boolean.TRUE.equals(request.getAttribute(RefreshLogoutHandler.UNAVAILABLE))) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            response.sendRedirect(request.getContextPath() + "/login?logout");
        }
    }
}
