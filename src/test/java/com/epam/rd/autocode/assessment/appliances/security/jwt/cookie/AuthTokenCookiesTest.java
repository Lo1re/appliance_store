package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.ApiBearerTokenResolver;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.ApiTransportProperties;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import static org.assertj.core.api.Assertions.*;

class AuthTokenCookiesTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void issuanceAndClearingUseTheSameScopeAndHttpsAlwaysUsesSecure() {
        var environment = environment(); environment.setProperty("server.servlet.context-path", "/shop");
        var store = new AuthTokenCookies(new AuthCookieProperties(false), clock, environment);
        var request = new MockHttpServletRequest(); request.setSecure(true);
        var response = new MockHttpServletResponse();
        store.issue(request, response, new AccessTokenResponse("test-jwt", "Bearer", 900, now.plusSeconds(899)));
        var issued = response.getCookie(AuthTokenCookies.NAME);
        assertThat(issued.getPath()).isEqualTo("/shop");
        assertThat(issued.isHttpOnly()).isTrue(); assertThat(issued.getSecure()).isTrue();
        assertThat(issued.getDomain()).isNull(); assertThat(issued.getMaxAge()).isEqualTo(899);
        assertThat(issued.getAttribute("SameSite")).isEqualTo("Lax");
        store.issueRefresh(request, response, "opaque-refresh", now.plusSeconds(7 * 86400));
        var refresh = response.getCookie(AuthTokenCookies.REFRESH_NAME);
        assertThat(refresh.getPath()).isEqualTo(issued.getPath()); assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getSecure()).isTrue(); assertThat(refresh.getMaxAge()).isEqualTo(7 * 86400);
        assertThat(refresh.getAttribute("SameSite")).isEqualTo("Lax");
        var cleared = new MockHttpServletResponse(); store.clear(request, cleared);
        assertThat(cleared.getCookie(AuthTokenCookies.NAME).getPath()).isEqualTo(issued.getPath());
        assertThat(cleared.getCookie(AuthTokenCookies.NAME).getMaxAge()).isZero();
        assertThat(cleared.getCookie(AuthTokenCookies.REFRESH_NAME).getMaxAge()).isZero();
        assertThat(cleared.getCookie(AuthTokenCookies.REFRESH_NAME).getPath()).isEqualTo(refresh.getPath());
    }

    @Test
    void refusesNonSecureProductionCookiesAndAmbiguousCredentials() {
        var environment = environment(); environment.setActiveProfiles("local", "prod");
        var properties = new ApiTransportProperties(ApiTransportProperties.Transport.COOKIE);
        assertThatThrownBy(() -> new AuthTokenCookies(new AuthCookieProperties(false), clock, environment)).isInstanceOf(IllegalArgumentException.class);
        var store = new AuthTokenCookies(new AuthCookieProperties(false), clock, environment());
        var request = new MockHttpServletRequest("GET", "/api/orders");
        request.setCookies(new Cookie(AuthTokenCookies.NAME, "first"), new Cookie(AuthTokenCookies.NAME, "second"));
        assertThatThrownBy(() -> new ApiBearerTokenResolver(properties, store).resolve(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
        request.setCookies(new Cookie(AuthTokenCookies.NAME, "first")); request.addHeader("Authorization", "Bearer second");
        assertThatThrownBy(() -> new ApiBearerTokenResolver(properties, store).resolve(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void staleCookieCannotPreventLoginCsrfBootstrapOrLogout() {
        var properties = new ApiTransportProperties(ApiTransportProperties.Transport.COOKIE);
        var resolver = new ApiBearerTokenResolver(properties, new AuthTokenCookies(new AuthCookieProperties(false), clock, environment()));
        for (String path : new String[]{"/api/auth/login", "/api/auth/logout", "/api/auth/csrf"}) {
            var request = new MockHttpServletRequest("POST", path);
            request.setCookies(new Cookie(AuthTokenCookies.NAME, "expired"));
            assertThat(resolver.resolve(request)).isNull();
        }
    }

    private MockEnvironment environment() { var environment = new MockEnvironment(); environment.setActiveProfiles("test"); return environment; }
}
