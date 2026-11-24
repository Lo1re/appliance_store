package com.epam.rd.autocode.assessment.appliances.controller.password;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.*;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityService;
import com.epam.rd.autocode.assessment.appliances.security.password.*;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetService;
import com.epam.rd.autocode.assessment.appliances.security.web.PasswordChangeLogoutHandler;
import com.epam.rd.autocode.assessment.appliances.service.password.PasswordChangeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Locale;

class PasswordChangeControllerBranchTest {
    final PasswordChangeService passwords = mock(PasswordChangeService.class);
    final CurrentActor actor = mock(CurrentActor.class);
    final PasswordCredentialStatusService state = mock(PasswordCredentialStatusService.class);
    final ObjectProvider<PasswordResetService> recovery = mock(ObjectProvider.class);
    final PasswordResetService reset = mock(PasswordResetService.class);
    final PasswordChangeController controller =
            new PasswordChangeController(
                    passwords,
                    mock(PasswordChangeLogoutHandler.class),
                    state,
                    actor,
                    mock(GoogleIdentityService.class),
                    recovery);
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final ExtendedModelMap model = new ExtendedModelMap();
    final RedirectAttributesModelMap redirects = new RedirectAttributesModelMap();
    final PasswordChangeRequest form = new PasswordChangeRequest();
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "passwordForm");

    @Test
    void legacyFormVisibilityDependsOnExplicitIntent() {
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "hash", null));
        for (var status : PasswordCredentialStatus.values()) {
            for (String method : new String[] {"GET", "POST"}) {
                for (String uri : new String[] {"/password/change", "/password/setup"}) {
                    for (String mode : new String[] {"change", "other"}) {
                        when(state.status(1L)).thenReturn(status);
                        request.setMethod(method);
                        request.setRequestURI(uri);
                        request.setParameter("mode", mode);
                        controller.accountContext(model, request);
                        boolean expected =
                                status == PasswordCredentialStatus.SET
                                        || status == PasswordCredentialStatus.UNKNOWN
                                                && (mode.equals("change")
                                                        || method.equals("POST")
                                                                && uri.equals("/password/change"));
                        assertThat(model.get("showPasswordChangeForm")).isEqualTo(expected);
                    }
                }
            }
        }
    }

    @Test
    void setupUnavailableWithoutRecoveryBean() {
        assertThatThrownBy(
                        () ->
                                controller.requestSetup(
                                        request, response, model, Locale.ENGLISH, redirects))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void setupUsesOnlyCurrentAccountAndShowsCooldown() {
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "hash", null));
        when(recovery.getIfAvailable()).thenReturn(reset);
        when(reset.request("client@example.test", request.getRemoteAddr(), Locale.ENGLISH))
                .thenReturn(42L);
        assertThat(controller.requestSetup(request, response, model, Locale.ENGLISH, redirects))
                .isEqualTo("password/change");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
    }

    @Test
    void setupOutageIsSanitized() {
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "hash", null));
        when(recovery.getIfAvailable()).thenReturn(reset);
        when(reset.request("client@example.test", request.getRemoteAddr(), Locale.ENGLISH))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(
                        () ->
                                controller.requestSetup(
                                        request, response, model, Locale.ENGLISH, redirects))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void absentPasswordRedirectsToSetup() {
        doThrow(new PasswordChangeException(PasswordChangeError.NOT_SET))
                .when(passwords)
                .change(form);
        assertThat(controller.change(form, errors, request, response, redirects))
                .isEqualTo("redirect:/password/change");
    }

    @Test
    void lockedPasswordChangeShows429WithoutLogout() {
        doThrow(new LockedException("private")).when(passwords).change(form);
        assertThat(controller.change(form, errors, request, response, redirects))
                .isEqualTo("password/change");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(errors.hasGlobalErrors()).isTrue();
    }

    @Test
    void changeOutageIsSanitized() {
        doThrow(new DataAccessResourceFailureException("private")).when(passwords).change(form);
        assertThatThrownBy(() -> controller.change(form, errors, request, response, redirects))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void unchangedPasswordShowsNewPasswordFieldError() {
        doThrow(new PasswordChangeException(PasswordChangeError.NEW_UNCHANGED))
                .when(passwords)
                .change(form);
        assertThat(controller.change(form, errors, request, response, redirects))
                .isEqualTo("password/change");
        assertThat(errors.hasFieldErrors("newPassword")).isTrue();
    }
}
