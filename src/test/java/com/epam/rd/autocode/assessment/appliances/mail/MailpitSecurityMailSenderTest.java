package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailpitSecurityMailSenderTest {
    @Test void sendsAccountAndPasswordNotificationsWithoutCredentials() {
        var transport = mock(JavaMailSender.class); var messages = new StaticMessageSource();
        messages.addMessage("mail.account-created.subject", Locale.ENGLISH, "Account created");
        messages.addMessage("mail.account-created.body", Locale.ENGLISH, "Your account was created.");
        messages.addMessage("mail.password-changed.subject", Locale.ENGLISH, "Password changed");
        messages.addMessage("mail.password-changed.body", Locale.ENGLISH, "Your password was changed.");
        var sender = new MailpitSecurityMailSender(transport, messages, "store@example.test");
        sender.sendAccountCreated("user@example.test", Locale.ENGLISH);
        sender.sendPasswordChanged("user@example.test", Locale.ENGLISH);
        var captured = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(transport, times(2)).send(captured.capture());
        assertThat(captured.getAllValues()).extracting(SimpleMailMessage::getSubject).containsExactly("Account created", "Password changed");
        assertThat(captured.getAllValues()).extracting(SimpleMailMessage::getText).containsExactly("Your account was created.", "Your password was changed.");
    }

    @Test void sendsVerificationWithItsOwnLocalizedTemplate() {
        var transport = mock(JavaMailSender.class); var messages = new StaticMessageSource();
        messages.addMessage("mail.verify.subject", Locale.ENGLISH, "Confirm email");
        messages.addMessage("mail.verify.body", Locale.ENGLISH, "Confirm: {0}");
        var sender = new MailpitSecurityMailSender(transport, messages, "store@example.test");
        sender.sendEmailVerification("user@example.test", URI.create("http://localhost:8080/onboarding/google/verify?token=secret"), Locale.ENGLISH);
        var captured = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(transport).send(captured.capture());
        assertThat(captured.getValue().getSubject()).isEqualTo("Confirm email");
        assertThat(captured.getValue().getText()).contains("/onboarding/google/verify?token=secret");
    }

    @Test void formatsLocalizedMailAndSanitizesTransportFailures() {
        var transport = mock(JavaMailSender.class);
        var messages = new StaticMessageSource();
        messages.addMessage("mail.reset.subject", Locale.ENGLISH, "Reset password");
        messages.addMessage("mail.reset.body", Locale.ENGLISH, "Reset: {0}");
        var sender = new MailpitSecurityMailSender(transport, messages, "store@example.test");
        URI link = URI.create("http://localhost:8080/password/reset?token=secret");
        sender.sendPasswordReset("user@example.test", link, Locale.ENGLISH);
        var captured = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(transport).send(captured.capture());
        assertThat(captured.getValue().getTo()).containsExactly("user@example.test");
        assertThat(captured.getValue().getText()).contains(link.toString());
        doThrow(new MailSendException("secret")).when(transport).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> sender.sendPasswordReset("user@example.test", link, Locale.ENGLISH))
                .isInstanceOf(MailDeliveryException.class).hasMessageNotContaining("secret")
                .satisfies(failure -> assertThat(failure.getCause()).hasMessageNotContaining("secret"));
    }
}
