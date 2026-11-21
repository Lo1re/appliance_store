package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Browser credential storage and attributes only; no JWT decoding or database operations. */
@Component
@ConditionalOnJwt
public class AuthTokenCookies {
    public static final String NAME = "store_access";
    public static final String REFRESH_NAME = "store_refresh";
    private final AuthCookieProperties properties;
    private final Clock clock;
    private final String path;

    public AuthTokenCookies(AuthCookieProperties properties, Clock clock, Environment environment) {
        this.properties = properties; this.clock = clock;
        if (!properties.secure() && (environment.acceptsProfiles(Profiles.of("prod"))
                || !environment.acceptsProfiles(Profiles.of("local", "test"))))
            throw new IllegalArgumentException("Non-secure authentication cookies are allowed only in local/test");
        String context = environment.getProperty("server.servlet.context-path", "");
        path = context.isEmpty() || context.equals("/") ? "/" : (context.endsWith("/") ? context.substring(0, context.length() - 1) : context);
    }
    public String path() { return path; }
    public String read(HttpServletRequest request) { return read(request, NAME); }
    public String readRefresh(HttpServletRequest request) { return read(request, REFRESH_NAME); }

    private String read(HttpServletRequest request, String name) {
        String value = null;
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) if (name.equals(cookie.getName())) {
            if (value != null) throw new IllegalArgumentException("Ambiguous authentication cookies");
            value = cookie.getValue();
        }
        if (value != null && (value.isBlank() || value.length() > 4096))
            throw new IllegalArgumentException("Invalid authentication cookie");
        return value;
    }

    public void issue(HttpServletRequest request, HttpServletResponse response, AccessTokenResponse token) {
        write(request, response, NAME, token.accessToken(), Math.min(token.expiresIn(), remaining(token.expiresAt())));
        clearLegacyAccess(request, response);
    }
    public void issueRefresh(HttpServletRequest request, HttpServletResponse response, String raw, Instant expiry) {
        write(request, response, REFRESH_NAME, raw, remaining(expiry));
    }
    public void clearAccess(HttpServletRequest request, HttpServletResponse response) {
        write(request, response, NAME, "", 0); clearLegacyAccess(request, response);
    }
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        clearAccess(request, response); write(request, response, REFRESH_NAME, "", 0);
    }
    private long remaining(Instant expiry) { return Math.max(0, Duration.between(clock.instant(), expiry).getSeconds()); }
    private void write(HttpServletRequest request, HttpServletResponse response, String name, String value, long seconds) {
        var cookie = new Cookie(name, value); cookie.setPath(path); cookie.setHttpOnly(true);
        cookie.setSecure(properties.secure() || request.isSecure()); cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(Math.toIntExact(Math.max(0, seconds))); response.addCookie(cookie);
    }
    private void clearLegacyAccess(HttpServletRequest request, HttpServletResponse response) {
        var legacy = new Cookie(NAME, ""); legacy.setPath((path.equals("/") ? "" : path) + "/api");
        legacy.setHttpOnly(true); legacy.setSecure(properties.secure() || request.isSecure());
        legacy.setAttribute("SameSite", "Lax"); legacy.setMaxAge(0); response.addCookie(legacy);
    }
}
