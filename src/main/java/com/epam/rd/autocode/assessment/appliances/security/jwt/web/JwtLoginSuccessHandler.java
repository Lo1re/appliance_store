package com.epam.rd.autocode.assessment.appliances.security.jwt.web;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.JwtCookieLifecycle;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** Exchanges a successful password authentication for the shared browser access cookie. */
@RequiredArgsConstructor
public final class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final RefreshTokenService tokens;
    private final JwtCookieLifecycle cookies;
    private final AuthTokenCookies browserCookies;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String previous;
        try { previous = browserCookies.readRefresh(request); } catch (IllegalArgumentException malformed) { previous = null; }
        cookies.issue(request, response, tokens.issue(authentication, previous));
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(request.getContextPath() + "/");
    }
}
