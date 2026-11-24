package com.epam.rd.autocode.assessment.appliances.controller.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.exception.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.*;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenService;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.flow.*;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.*;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;

import org.junit.jupiter.api.Test;
import org.springframework.dao.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.*;

class GoogleOnboardingControllerBranchTest {
    final GoogleFlowSession flows = mock(GoogleFlowSession.class);
    final GoogleEmailVerification verification = mock(GoogleEmailVerification.class);
    final GoogleIdentityService identities = mock(GoogleIdentityService.class);
    final GoogleOnboardingController controller =
            new GoogleOnboardingController(
                    flows,
                    verification,
                    identities,
                    mock(GoogleAccountAuthentication.class),
                    mock(RefreshTokenService.class),
                    mock(JwtCookieLifecycle.class),
                    mock(AuthTokenCookies.class),
                    new AccountFormView());
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final ExtendedModelMap model = new ExtendedModelMap();
    final RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
    final GoogleProfileRequest form = new GoogleProfileRequest();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "googleProfile");

    String complete() throws Exception {
        return controller.complete(
                form, errors, request, response, model, Locale.ENGLISH, redirects);
    }

    void pending() {
        when(flows.requirePending(request))
                .thenReturn(
                        new VerifiedGoogleIdentity("subject", "user@example.test", "Client", true));
    }

    @Test
    void existingDraftIsPreserved() {
        pending();
        when(flows.draft(request)).thenReturn(Optional.of(form));
        assertThat(controller.form(request, response, model, redirects))
                .isEqualTo("oauth/google-profile");
        assertThat(model.get("googleProfile")).isSameAs(form);
    }

    @Test
    void expiredFormClearsWorkflow() {
        when(flows.requirePending(request))
                .thenThrow(new GoogleLoginException(GoogleLoginException.Reason.FLOW_EXPIRED));
        assertThat(controller.form(request, response, model, redirects))
                .isEqualTo("redirect:/login?googleExpired");
        verify(flows).finish(request, response);
    }

    @Test
    void invalidFormNeverCreatesAccount() throws Exception {
        pending();
        errors.reject("invalid");
        assertThat(complete()).isEqualTo("oauth/google-profile");
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(identities);
    }

    @Test
    void verificationCooldownShowsRemainingTime() throws Exception {
        pending();
        when(verification.send(request, null, Locale.ENGLISH)).thenReturn(17L);
        assertThat(complete()).isEqualTo("oauth/google-profile");
        assertThat(response.getHeader("Retry-After")).isEqualTo("17");
    }

    @Test
    void typedAndDatabaseConflictsAreSafe() throws Exception {
        pending();
        when(flows.emailVerified(request, null)).thenReturn(true);
        when(identities.createProfile(any(), eq(form)))
                .thenThrow(new AccountException(AccountError.CARD_DUPLICATE));
        assertThat(complete()).isEqualTo("oauth/google-profile");
        assertThat(errors.hasFieldErrors("card")).isTrue();
        when(identities.createProfile(any(), eq(form)))
                .thenThrow(new DataIntegrityViolationException("private"));
        var databaseErrors = new BeanPropertyBindingResult(form, "googleProfile");
        assertThat(
                        controller.complete(
                                form,
                                databaseErrors,
                                request,
                                response,
                                model,
                                Locale.ENGLISH,
                                redirects))
                .isEqualTo("oauth/google-profile");
        assertThat(databaseErrors.hasGlobalErrors()).isTrue();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void serviceOutageIsSanitized() throws Exception {
        pending();
        when(flows.emailVerified(request, null)).thenReturn(true);
        when(identities.createProfile(any(), eq(form)))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(this::complete).isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void nonLinkRejectionExpiresWorkflow() throws Exception {
        when(flows.requirePending(request))
                .thenThrow(new GoogleLoginException(GoogleLoginException.Reason.INVALID_IDENTITY));
        assertThat(complete()).isEqualTo("redirect:/login?googleExpired");
    }

    @Test
    void existingEmailRequiresExplicitLinkAndCancelClearsFlow() throws Exception {
        when(flows.requirePending(request))
                .thenThrow(new GoogleLoginException(GoogleLoginException.Reason.LINK_REQUIRED));
        assertThat(complete()).isEqualTo("redirect:/login?googleLinkRequired");
        assertThat(controller.cancel(request, response, redirects)).isEqualTo("redirect:/login");
    }
}
