package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

// Revokes this browser's refresh credential, then clears its matching cookie scope.
@RequiredArgsConstructor
public final class RefreshLogoutHandler implements LogoutHandler {
    static final String UNAVAILABLE = RefreshLogoutHandler.class.getName() + ".unavailable";
    private final RefreshTokenService refresh;
    private final AuthTokenCookies cookies;
    private final JwtCookieLifecycle lifecycle;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        try {
            String raw;
            try {
                raw = cookies.readRefresh(request);
            } catch (IllegalArgumentException malformed) {
                raw = null;
            }
            refresh.revoke(raw);
        } catch (AuthenticationServiceException unavailable) {
            request.setAttribute(UNAVAILABLE, Boolean.TRUE);
        } finally {
            lifecycle.clear(request, response);
        }
    }
}
