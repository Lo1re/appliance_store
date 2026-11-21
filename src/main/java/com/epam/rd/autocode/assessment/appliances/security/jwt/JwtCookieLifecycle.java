package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

/** Rotates browser authentication cookies and invalidates pre-authentication CSRF tokens. */
@Component
@RequiredArgsConstructor
@ConditionalOnJwt
public class JwtCookieLifecycle {
    private final AccessTokenCookies access;
    private final CookieCsrfTokenRepository csrf;

    public void issue(HttpServletRequest request, HttpServletResponse response, AccessTokenResponse token) {
        access.issue(request, response, token); csrf.saveToken(null, request, response);
    }
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        access.clear(request, response); csrf.saveToken(null, request, response);
    }
}
