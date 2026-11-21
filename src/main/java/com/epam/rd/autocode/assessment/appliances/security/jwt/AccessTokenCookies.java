package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Access-cookie attributes and storage only; credentials and JWT decoding live elsewhere. */
@Component
@ConditionalOnJwt
public class AccessTokenCookies {
    public static final String NAME = "store_access";
    private final AuthCookieProperties properties;
    private final Clock clock;
    private final String path;

    public AccessTokenCookies(AuthCookieProperties properties, Clock clock, Environment environment) {
        this.properties = properties; this.clock = clock;
        if (!properties.secure() && (environment.acceptsProfiles(Profiles.of("prod"))
                || !environment.acceptsProfiles(Profiles.of("local", "test")))) {
            throw new IllegalArgumentException("Non-secure API cookies are allowed only in local/test");
        }
        String context = environment.getProperty("server.servlet.context-path", "");
        path = context.isEmpty() || context.equals("/") ? "/" : (context.endsWith("/") ? context.substring(0, context.length() - 1) : context);
    }

    public String path() { return path; }

    public String read(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (NAME.equals(cookie.getName())) {
                if (token != null) throw new IllegalArgumentException("Ambiguous access cookies");
                token = cookie.getValue();
            }
        }
        if (token != null && (token.isBlank() || token.length() > 4096)) {
            throw new IllegalArgumentException("Invalid access cookie");
        }
        return token;
    }

    public void issue(HttpServletRequest request, HttpServletResponse response, AccessTokenResponse token) {
        long seconds = Math.max(0, Math.min(token.expiresIn(), Duration.between(clock.instant(), token.expiresAt()).getSeconds()));
        write(request, response, token.accessToken(), seconds);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) { write(request, response, "", 0); }

    private void write(HttpServletRequest request, HttpServletResponse response, String value, long seconds) {
        var cookie = new Cookie(NAME, value);
        cookie.setPath(path); cookie.setHttpOnly(true);
        cookie.setSecure(properties.secure() || request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(Math.toIntExact(seconds));
        response.addCookie(cookie);
        // Retire the earlier API-only cookie to avoid two credentials with the same name.
        var legacy = new Cookie(NAME, "");
        legacy.setPath((path.equals("/") ? "" : path) + "/api");
        legacy.setHttpOnly(true); legacy.setSecure(cookie.getSecure());
        legacy.setAttribute("SameSite", "Lax"); legacy.setMaxAge(0);
        response.addCookie(legacy);
    }
}
