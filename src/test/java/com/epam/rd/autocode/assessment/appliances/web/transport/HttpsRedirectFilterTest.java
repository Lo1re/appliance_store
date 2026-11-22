package com.epam.rd.autocode.assessment.appliances.web.transport;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;

class HttpsRedirectFilterTest {
    @Test void hostAndForwardedHeadersCannotOverrideTrustedRedirectOrigin() throws Exception {
        var request = new MockHttpServletRequest("HEAD", "/login"); request.setServerName("attacker.example"); request.addHeader("X-Forwarded-Proto", "https");
        var response = new MockHttpServletResponse();
        new HttpsRedirectFilter("https://localhost:8443/").doFilter(request, response, (req,res) -> { throw new AssertionError("HTTP must not reach authentication"); });
        assertThat(response.getHeader("Location")).isEqualTo("https://localhost:8443/login"); assertThat(response.getStatus()).isEqualTo(308);
    }
    @Test void httpsRequestsContinueWithoutRedirect() throws Exception {
        var request = new MockHttpServletRequest("GET", "/login"); request.setSecure(true); var response = new MockHttpServletResponse();
        new HttpsRedirectFilter("https://localhost:8443").doFilter(request, response, (req,res) -> req.setAttribute("continued",true));
        assertThat(request.getAttribute("continued")).isEqualTo(true); assertThat(response.getHeader("Location")).isNull();
    }
    @Test void rejectsOriginsWithInsecureSchemeCredentialsOrPaths() {
        for(String origin : new String[]{"http://localhost:8443", "https://user:password@localhost", "https://localhost/path", "https://localhost?query", "https://localhost#fragment"})
            assertThatThrownBy(() -> new HttpsRedirectFilter(origin)).isInstanceOf(IllegalArgumentException.class);
    }
}
