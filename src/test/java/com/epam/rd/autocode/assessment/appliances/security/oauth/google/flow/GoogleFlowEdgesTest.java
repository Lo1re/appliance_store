package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.time.*;

class GoogleFlowEdgesTest {
    final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void incompleteAndExpiredWorkflowAttributesAreRejected() {
        var flows = new GoogleFlowSession(clock);
        var request = new MockHttpServletRequest();
        request.getSession()
                .setAttribute(GoogleFlowSession.class.getName() + ".pending", "unexpected");
        assertThatThrownBy(() -> flows.requirePending(request))
                .isInstanceOf(GoogleLoginException.class);
        flows.pending(
                request, new VerifiedGoogleIdentity("sub", "email@example.test", "User", false));
        assertThat(flows.emailVerified(request, null)).isFalse();
        assertThat(flows.draft(request)).isEmpty();
        var form = new GoogleProfileRequest();
        form.setName("Draft");
        flows.saveDraft(request, form);
        assertThat(flows.draft(request)).isPresent();
        flows.beginLink(request, 1L, "hash");
        assertThatThrownBy(
                        () ->
                                new GoogleFlowSession(Clock.offset(clock, Duration.ofMinutes(16)))
                                        .link(request))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void expiredAndWrongTypedLinksCannotBeUsed() {
        var request = new MockHttpServletRequest();
        var flows = new GoogleFlowSession(clock);
        request.getSession()
                .setAttribute(GoogleFlowSession.class.getName() + ".link", "unexpected");
        assertThatThrownBy(() -> flows.link(request)).isInstanceOf(GoogleLoginException.class);
        flows.beginLink(request, 1L, "hash");
        assertThatThrownBy(
                        () ->
                                new GoogleFlowSession(Clock.offset(clock, Duration.ofMinutes(16)))
                                        .link(request))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void authorizationRepositoryHonorsStateExpiryAndRemoval() {
        var repo = new GoogleAuthorizationRequestRepository(clock);
        var r = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        assertThat(repo.loadAuthorizationRequest(r)).isNull();
        repo.saveAuthorizationRequest(null, r, response);
        assertThat(repo.removeAuthorizationRequest(r, response)).isNull();
        var auth =
                OAuth2AuthorizationRequest.authorizationCode()
                        .authorizationUri("https://accounts.google.com/auth")
                        .clientId("client")
                        .redirectUri("https://localhost/login/oauth2/code/google")
                        .state("state")
                        .build();
        repo.saveAuthorizationRequest(auth, r, response);
        r.setParameter("state", "state");
        assertThat(repo.loadAuthorizationRequest(r)).isSameAs(auth);
        assertThat(repo.removeAuthorizationRequest(r, response)).isSameAs(auth);
        repo.saveAuthorizationRequest(auth, r, response);
        repo.saveAuthorizationRequest(null, r, response);
        assertThat(repo.loadAuthorizationRequest(r)).isNull();
        repo.saveAuthorizationRequest(auth, r, response);
        r.getSession()
                .setAttribute(
                        GoogleAuthorizationRequestRepository.class.getName() + ".until", "wrong");
        assertThat(repo.loadAuthorizationRequest(r)).isNull();
        repo.saveAuthorizationRequest(auth, r, response);
        var expired =
                new GoogleAuthorizationRequestRepository(
                        Clock.offset(clock, Duration.ofMinutes(16)));
        assertThat(expired.loadAuthorizationRequest(r)).isNull();
    }
}
