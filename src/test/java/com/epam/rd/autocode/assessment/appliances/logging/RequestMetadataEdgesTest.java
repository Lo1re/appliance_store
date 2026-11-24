package com.epam.rd.autocode.assessment.appliances.logging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.logging.SecurityLogContext;
import com.epam.rd.autocode.assessment.appliances.web.LanguageNavigation;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.HandlerMapping;

class RequestMetadataEdgesTest {
    @Test
    void unsafeMethodPathsAndPatternsDoNotEnterLogs() {
        for (String method : new String[] {null, "injected\nmethod", "x".repeat(17)}) {
            var r = mock(HttpServletRequest.class);
            when(r.getMethod()).thenReturn(method);
            assertThat(HttpLogMetadata.capture(r).method()).isEqualTo("OTHER");
        }
        for (String path :
                new String[] {
                    null,
                    "relative",
                    "/" + "x".repeat(257),
                    "/plain?private=value",
                    "/plain#private",
                    "/a;session=private//b"
                }) {
            var r = new MockHttpServletRequest();
            r.setRequestURI(path);
            assertThat(HttpLogMetadata.capture(r).path()).doesNotContain("private");
        }
        for (String pattern :
                new String[] {
                    "x".repeat(257),
                    "/bad\rvalue",
                    "/bad\nvalue",
                    "/bad?query",
                    "/bad#fragment",
                    "/plain/*"
                }) {
            var r = new MockHttpServletRequest("GET", "/safe");
            r.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
            assertThat(HttpLogMetadata.capture(r).path()).isEqualTo("/safe");
        }
    }

    @Test
    void languageLinksRejectUnsafeOriginsAndLogIdentityIgnoresAnonymousDetails() {
        var links = new LanguageNavigation();
        for (String path :
                new String[] {null, "relative", "//remote", "/bad\rvalue", "/bad\nvalue"}) {
            var r = new MockHttpServletRequest();
            r.setRequestURI(path);
            assertThat(links.links(r).en()).isEqualTo("/?lang=en");
        }
        var r = new MockHttpServletRequest("GET", "/safe");
        r.setDispatcherType(DispatcherType.ERROR);
        assertThat(links.links(r).uk()).isEqualTo("/?lang=uk");
        assertThat(SecurityLogContext.accountId(null)).isNull();
        assertThat(
                        SecurityLogContext.accountId(
                                UsernamePasswordAuthenticationToken.unauthenticated(
                                        "name", "secret")))
                .isNull();
        assertThat(SecurityLogContext.safeRemoteAddress(null)).isEqualTo("-");
        RuntimeException deepest = new RuntimeException("secret");
        RuntimeException top = deepest;
        for (int i = 0; i < 12; i++) {
            top = new RuntimeException("secret", top);
        }
        assertThat(SafeFailure.snapshot(top)).hasMessage(RuntimeException.class.getName());
    }
}
