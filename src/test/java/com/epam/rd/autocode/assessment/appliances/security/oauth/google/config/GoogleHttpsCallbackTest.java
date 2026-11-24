package com.epam.rd.autocode.assessment.appliances.security.oauth.google.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class GoogleHttpsCallbackTest {
    GoogleLoginProperties properties(String callback) {
        var properties = new GoogleLoginProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.setRedirectUri(callback);
        return properties;
    }

    @Test
    void tlsEnabledLocalServerRejectsOldHttpCallback() {
        var environment = new MockEnvironment().withProperty("server.ssl.enabled", "true");
        environment.setActiveProfiles("local");
        assertThatThrownBy(
                        () ->
                                new GoogleLoginConfiguration()
                                        .googleClientRegistrations(
                                                properties(
                                                        "http://localhost:8080/login/oauth2/code/google"),
                                                environment))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tlsEnabledLocalServerAcceptsExactHttpsCallback() {
        var environment = new MockEnvironment().withProperty("server.ssl.enabled", "true");
        environment.setActiveProfiles("local");
        var registrations =
                new GoogleLoginConfiguration()
                        .googleClientRegistrations(
                                properties("https://localhost:8443/login/oauth2/code/google"),
                                environment);
        assertThat(registrations.findByRegistrationId("google").getRedirectUri())
                .isEqualTo("https://localhost:8443/login/oauth2/code/google");
    }

    @Test
    void nonTlsTestProfileStillSupportsItsExplicitHttpFixture() {
        var environment = new MockEnvironment().withProperty("server.ssl.enabled", "false");
        environment.setActiveProfiles("test");
        assertThat(
                        new GoogleLoginConfiguration()
                                .googleClientRegistrations(
                                        properties(
                                                "http://localhost:8080/login/oauth2/code/google"),
                                        environment))
                .isNotNull();
    }
}
