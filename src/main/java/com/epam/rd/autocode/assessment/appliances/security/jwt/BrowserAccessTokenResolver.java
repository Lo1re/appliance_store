package com.epam.rd.autocode.assessment.appliances.security.jwt;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/** HTML requests accept only the browser cookie, independently of external API header mode. */
@RequiredArgsConstructor
public final class BrowserAccessTokenResolver implements BearerTokenResolver {
    private final AccessTokenCookies cookies;

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (request.getDispatcherType() == DispatcherType.ERROR || path.equals("/login") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")) return null;
        try { return cookies.read(request); }
        catch (IllegalArgumentException malformed) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request", "Invalid browser credentials", null));
        }
    }
}
