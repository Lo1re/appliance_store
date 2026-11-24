package com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.mail.*;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.config.GoogleLoginProperties;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.util.Locale;

class GoogleVerificationFailureEdgesTest {
    @Test
    void unavailableDeliveryAndThrottlingNeverIssueChallenges() {
        var flows = mock(GoogleFlowSession.class);
        var limits = mock(ResetRequestLimiter.class);
        var mail = mock(ObjectProvider.class);
        var events = mock(ApplicationEventPublisher.class);
        var properties = new GoogleLoginProperties();
        properties.setRedirectUri("https://localhost/login/oauth2/code/google");
        var tokens = new ResetTokenCodec();
        var verification =
                new GoogleEmailVerification(flows, limits, tokens, properties, mail, events);
        var request = new MockHttpServletRequest();
        assertThatThrownBy(() -> verification.send(request, "user@example.test", Locale.ENGLISH))
                .isInstanceOf(AuthenticationServiceException.class);
        when(mail.getIfAvailable()).thenReturn(mock(SecurityMailSender.class));
        when(limits.acquire(any(), any())).thenReturn(17L);
        assertThat(verification.send(request, "user@example.test", Locale.ENGLISH)).isEqualTo(17);
        verifyNoInteractions(events);
        assertThatThrownBy(() -> verification.confirm(request, "bad"))
                .isInstanceOf(GoogleLoginException.class);
        verification.confirm(request, tokens.generate());
        verify(flows).verifyEmail(eq(request), anyString());
    }
}
