package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;

import java.time.*;

class AuthenticationCookieEdgesTest {
    AuthTokenCookies cookies(String path, boolean secure, String... profiles) {
        var env = new MockEnvironment().withProperty("server.servlet.context-path", path);
        env.setActiveProfiles(profiles);
        return new AuthTokenCookies(new AuthCookieProperties(secure), Clock.systemUTC(), env);
    }

    @Test
    void insecureCookiesAreRejectedOutsideExplicitLocalTestProfiles() {
        assertThatThrownBy(() -> cookies("", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cookies("", false, "prod", "local"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cookies("/", true).path()).isEqualTo("/");
        assertThat(cookies("/shop/", true).path()).isEqualTo("/shop");
        assertThat(cookies("/shop", true).path()).isEqualTo("/shop");
    }

    @Test
    void oversizedBlankAndAmbiguousCredentialsCannotBeRead() {
        var cookies = cookies("", false, "test");
        for (String value : new String[] {" ", "x".repeat(4097)}) {
            var r = new MockHttpServletRequest();
            r.setCookies(new Cookie(AuthTokenCookies.NAME, value));
            assertThatThrownBy(() -> cookies.read(r)).isInstanceOf(IllegalArgumentException.class);
        }
        var r = new MockHttpServletRequest();
        r.setCookies(
                new Cookie(AuthTokenCookies.NAME, "first"),
                new Cookie(AuthTokenCookies.NAME, "second"));
        assertThatThrownBy(() -> cookies.read(r)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void secureTransportForcesSecureLegacyDeletionUnderContextPath() {
        var c = cookies("/shop/", false, "test");
        var r = new MockHttpServletRequest();
        r.setSecure(true);
        var response = new MockHttpServletResponse();
        c.clear(r, response);
        assertThat(response.getCookies()).allMatch(Cookie::getSecure);
        assertThat(response.getCookies())
                .extracting(Cookie::getPath)
                .contains("/shop/api", "/shop");
    }
}
