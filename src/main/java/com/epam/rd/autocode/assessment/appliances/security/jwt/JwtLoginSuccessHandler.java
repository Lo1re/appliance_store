package com.epam.rd.autocode.assessment.appliances.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** Exchanges a successful password authentication for the shared browser access cookie. */
@RequiredArgsConstructor
public final class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AccessTokenIssuer tokens;
    private final JwtCookieLifecycle cookies;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        cookies.issue(request, response, tokens.issue(authentication));
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(request.getContextPath() + "/");
    }
}
