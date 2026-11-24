package com.epam.rd.autocode.assessment.appliances.security.oauth.google.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GoogleCallbackPolicyEdgesTest {
    GoogleLoginProperties properties(String uri) {
        var p = new GoogleLoginProperties();
        p.setClientId("test-id");
        p.setClientSecret("test-secret");
        p.setRedirectUri(uri);
        return p;
    }

    void rejects(String uri, MockEnvironment env) {
        assertThatThrownBy(
                        () ->
                                new GoogleLoginConfiguration()
                                        .googleClientRegistrations(properties(uri), env))
                .as(uri)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsModesAndUnsafeCallbackComponents() {
        rejects(
                "https://example.test/login/oauth2/code/google",
                new MockEnvironment().withProperty("store.security.web-auth", "session"));
        rejects(
                "https://example.test/login/oauth2/code/google",
                new MockEnvironment().withProperty("store.security.auth-mode", "memory"));
        for (String uri :
                new String[] {
                    "https:/login/oauth2/code/google",
                    "https://user@example.test/login/oauth2/code/google",
                    "https://example.test/login/oauth2/code/google?q=1",
                    "https://example.test/login/oauth2/code/google#x",
                    "https://example.test/wrong"
                }) {
            rejects(uri, new MockEnvironment());
        }
        rejects(
                "http://remote.test/login/oauth2/code/google",
                new MockEnvironment().withProperty("server.ssl.enabled", "false"));
        rejects("http://localhost/login/oauth2/code/google", new MockEnvironment());
        var production = new MockEnvironment();
        production.setActiveProfiles("local", "prod");
        rejects("http://localhost/login/oauth2/code/google", production);
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        assertThat(
                        new GoogleLoginConfiguration()
                                .googleClientRegistrations(
                                        properties("http://127.0.0.1/login/oauth2/code/google"),
                                        local))
                .isNotNull();
        assertThat(properties("https://localhost/login/oauth2/code/google").toString())
                .doesNotContain("test-secret");
    }
}
