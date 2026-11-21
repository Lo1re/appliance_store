package com.epam.rd.autocode.assessment.appliances.security.jwt.api;

import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/** Uses one configured transport; URL/form parameters and mixed credentials are not accepted. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiBearerTokenResolver implements BearerTokenResolver {
    private final ApiTransportProperties properties;
    private final AuthTokenCookies cookies;
    private final DefaultBearerTokenResolver headers = new DefaultBearerTokenResolver();

    @Override public String resolve(HttpServletRequest request) {
        if (properties.cookieMode() && request.getHeader(HttpHeaders.AUTHORIZATION) != null) throw invalid();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/api/auth/login") || path.equals("/api/auth/logout") || path.equals("/api/auth/refresh") || path.equals("/api/auth/csrf")) return null;
        if (!properties.cookieMode()) return headers.resolve(request);
        try { return cookies.read(request); }
        catch (IllegalArgumentException malformed) { throw invalid(); }
    }
    private OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_request", "Invalid API credentials", null));
    }
}
