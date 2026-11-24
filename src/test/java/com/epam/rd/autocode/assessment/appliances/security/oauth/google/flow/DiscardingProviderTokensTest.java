package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class DiscardingProviderTokensTest {
    @Test
    void providerCredentialsAreNeverRetainedOrPlacedInASession() {
        var repository = new DiscardingAuthorizedClientRepository();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        repository.saveAuthorizedClient(null, null, request, response);
        assertThat((Object) repository.loadAuthorizedClient("google", null, request)).isNull();
        repository.removeAuthorizedClient("google", null, request, response);
        assertThat(request.getSession(false)).isNull();
        assertThat(response.getContentAsByteArray()).isEmpty();
    }
}
