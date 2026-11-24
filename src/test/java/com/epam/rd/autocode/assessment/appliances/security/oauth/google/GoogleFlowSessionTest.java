package com.epam.rd.autocode.assessment.appliances.security.oauth.google;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.VerifiedGoogleIdentity;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;

class GoogleFlowSessionTest {
    @Test
    void pendingIdentityHasHardExpiryAndDoesNotConfirmAnEditedEmail() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        var flows = new GoogleFlowSession(clock);
        var request = new MockHttpServletRequest();
        var identity = new VerifiedGoogleIdentity("subject", "first@example.test", "Name", true);
        flows.pending(request, identity);
        assertThat(flows.emailVerified(request, "first@example.test")).isTrue();
        assertThat(flows.emailVerified(request, "other@example.test")).isFalse();
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(900));
        assertThatThrownBy(() -> flows.requirePending(request))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void emailChallengeIsSessionBoundSingleUseAndExpires() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        var flows = new GoogleFlowSession(clock);
        var request = new MockHttpServletRequest();
        flows.pending(
                request,
                new VerifiedGoogleIdentity("subject", "first@example.test", "Name", false));
        flows.challenge(request, "other@example.test", "digest");
        assertThatThrownBy(() -> flows.verifyEmail(new MockHttpServletRequest(), "digest"))
                .isInstanceOf(GoogleLoginException.class);
        assertThatThrownBy(() -> flows.verifyEmail(request, "wrong"))
                .isInstanceOf(GoogleLoginException.class);
        flows.verifyEmail(request, "digest");
        assertThat(flows.emailVerified(request, "other@example.test")).isTrue();
        assertThatThrownBy(() -> flows.verifyEmail(request, "digest"))
                .isInstanceOf(GoogleLoginException.class);
        flows.challenge(request, "third@example.test", "next-digest");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(600));
        assertThatThrownBy(() -> flows.verifyEmail(request, "next-digest"))
                .isInstanceOf(GoogleLoginException.class);
        flows.finish(request, new MockHttpServletResponse());
        assertThat(request.getSession(false)).isNull();
    }
}
