package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;

/** Writes or clears a credential pair and rotates the associated CSRF cookie. */
@Component
@RequiredArgsConstructor
@ConditionalOnJwt
public class JwtCookieLifecycle {
    private final AuthTokenCookies cookies;
    private final CookieCsrfTokenRepository csrf;
    public void issue(HttpServletRequest request, HttpServletResponse response, TokenPair pair) {
        cookies.issue(request, response, pair.access());
        cookies.issueRefresh(request, response, pair.refreshToken(), pair.refreshExpiresAt());
        csrf.saveToken(null, request, response);
    }
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        cookies.clear(request, response); csrf.saveToken(null, request, response);
    }
}
