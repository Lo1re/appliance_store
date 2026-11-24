package com.epam.rd.autocode.assessment.appliances.controller.registration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest;
import com.epam.rd.autocode.assessment.appliances.exception.*;
import com.epam.rd.autocode.assessment.appliances.security.registration.*;
import com.epam.rd.autocode.assessment.appliances.web.form.AccountFormView;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.Locale;

class ClientRegistrationControllerBranchTest {
    final RegistrationSubmission submissions = mock(RegistrationSubmission.class);
    final RegistrationConfirmation confirmations = mock(RegistrationConfirmation.class);
    final ClientRegistrationController controller =
            new ClientRegistrationController(submissions, confirmations, new AccountFormView());
    final ClientRegistrationRequest form = new ClientRegistrationRequest();
    final BeanPropertyBindingResult errors =
            new BeanPropertyBindingResult(form, "registrationForm");
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final ExtendedModelMap model = new ExtendedModelMap();

    String submit() {
        return controller.request(form, errors, request, response, model, Locale.ENGLISH);
    }

    @Test
    void invalidInputDoesNotIssueApplication() {
        errors.reject("invalid");
        assertThat(submit()).isEqualTo("registration/form");
        assertThat(response.getStatus()).isEqualTo(400);
        verifyNoInteractions(submissions);
    }

    @Test
    void fieldConflictIsDisplayedWithoutPassword() {
        when(submissions.submit(any(), any(), any()))
                .thenThrow(new AccountException(AccountError.CARD_DUPLICATE));
        assertThat(submit()).isEqualTo("registration/form");
        assertThat(errors.hasFieldErrors("card")).isTrue();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void databaseFailureIsSanitized() {
        when(submissions.submit(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(this::submit)
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageNotContaining("private");
    }

    @Test
    void previewOutageIsSanitized() {
        when(confirmations.preview("token"))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(() -> controller.verify("token", model))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void completionConflictPreservesProfileAndShowsFieldError() {
        when(confirmations.complete("token"))
                .thenThrow(new AccountException(AccountError.EMAIL_DUPLICATE));
        when(confirmations.preview("token"))
                .thenReturn(
                        new RegistrationDetails(
                                "Client", "client@example.test", null, "private-hash"));
        assertThat(controller.complete("token", response, model)).isEqualTo("registration/form");
        var displayed = (ClientRegistrationRequest) model.get("registrationForm");
        assertThat(displayed.getEmail()).isEqualTo("client@example.test");
        assertThat(displayed.getNewPassword()).isNull();
        var result =
                (org.springframework.validation.BindingResult)
                        model.get(
                                org.springframework.validation.BindingResult.MODEL_KEY_PREFIX
                                        + "registrationForm");
        assertThat(result.hasFieldErrors("email")).isTrue();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void conflictWhoseApplicationExpiredUsesSafeRedirect() {
        when(confirmations.complete("token"))
                .thenThrow(new AccountException(AccountError.CARD_DUPLICATE));
        when(confirmations.preview("token")).thenThrow(new InvalidRegistrationException());
        assertThat(controller.complete("token", response, model))
                .isEqualTo("redirect:/register?invalid");
    }

    @Test
    void storageOutageAfterConflictIsAlsoSanitized() {
        when(confirmations.complete("token"))
                .thenThrow(new AccountException(AccountError.EMAIL_DUPLICATE));
        when(confirmations.preview("token"))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(() -> controller.complete("token", response, model))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void completionStorageOutageIsSanitized() {
        when(confirmations.complete("token"))
                .thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(() -> controller.complete("token", response, model))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void checkEmailPageDoesNotExposePendingData() {
        assertThat(controller.checkEmail()).isEqualTo("registration/check-email");
        verifyNoInteractions(confirmations);
    }
}
