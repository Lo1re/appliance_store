package com.epam.rd.autocode.assessment.appliances.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import static org.assertj.core.api.Assertions.*;

class HttpLogMetadataTest {
    @Test
    void unknownAddressIsVisibleButItsQueryParametersAreNot() {
        var request = new MockHttpServletRequest("GET", "/appliances1");
        request.setQueryString("password=private-password");
        var metadata = HttpLogMetadata.capture(request);
        assertThat(metadata.path()).isEqualTo("/appliances1");
        assertThat(metadata.route()).isEqualTo("-");
    }

    @Test
    void knownRoutesUseTheTemplateInsteadOfActualPathParameters() {
        var request = new MockHttpServletRequest("GET", "/orders/123");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/orders/{id}");
        assertThat(HttpLogMetadata.capture(request).path()).isEqualTo("/orders/{id}");
    }

    @Test
    void sensitiveLongEncodedAndMatrixSegmentsAreRedacted() {
        assertPath("/reset-password/private-token", "/reset-password/[redacted]");
        assertPath("/unknown/123456789012345678901234567890123456789", "/unknown/[redacted]");
        assertPath("/unknown/%0Aprivate-value", "/unknown/[redacted]");
        assertPath("/typo;jsessionid=private-session", "/typo");
        assertPath("/typo?token=private-token", "/typo");
    }

    private void assertPath(String uri, String expected) {
        var request = new MockHttpServletRequest("GET", uri);
        assertThat(HttpLogMetadata.capture(request).path()).isEqualTo(expected);
    }
}
