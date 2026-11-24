package com.epam.rd.autocode.assessment.appliances.controller.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthTokenCookies;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.GoogleFlowSession;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GoogleLinkControllerBranchTest {
    @Test
    void refusesLinkingWithoutCurrentAccessCredential() {
        var actor = mock(CurrentActor.class);
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "hash", null));
        var flows = mock(GoogleFlowSession.class);
        var controller =
                new GoogleLinkController(
                        actor,
                        mock(GoogleIdentityService.class),
                        flows,
                        mock(AuthTokenCookies.class),
                        new ResetTokenCodec());
        assertThatThrownBy(() -> controller.begin(new MockHttpServletRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(flows);
    }

    @Test
    void accountPageReflectsActualLinkState() {
        var actor = mock(CurrentActor.class);
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "hash", null));
        var identities = mock(GoogleIdentityService.class);
        when(identities.linked(1L)).thenReturn(true);
        var model = new org.springframework.ui.ExtendedModelMap();
        var controller =
                new GoogleLinkController(
                        actor,
                        identities,
                        mock(GoogleFlowSession.class),
                        mock(AuthTokenCookies.class),
                        new ResetTokenCodec());
        assertThat(controller.page(model)).isEqualTo("oauth/google-link");
        assertThat(model.get("googleLinked")).isEqualTo(true);
    }
}
