package com.epam.rd.autocode.assessment.appliances.security.oauth.google;

import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleAuthorizationRequestRepository;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class GoogleAuthorizationRequestRepositoryTest {
    @Test void requiresMatchingStateAndRejectsExpiredAuthorization() {
        var clock = mock(Clock.class); when(clock.instant()).thenReturn(Instant.EPOCH);
        var repo = new GoogleAuthorizationRequestRepository(clock);
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        var authorization = OAuth2AuthorizationRequest.authorizationCode().authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client").redirectUri("http://localhost:8080/login/oauth2/code/google").state("expected-state").build();
        repo.saveAuthorizationRequest(authorization, request, response);
        var callback = new MockHttpServletRequest(); callback.setSession((MockHttpSession) request.getSession());
        callback.setParameter("state", "wrong-state"); assertThat(repo.loadAuthorizationRequest(callback)).isNull();
        callback.setParameter("state", "expected-state"); assertThat(repo.loadAuthorizationRequest(callback)).isNotNull();
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(900));
        assertThat(repo.removeAuthorizationRequest(callback, response)).isNull();
    }
}
