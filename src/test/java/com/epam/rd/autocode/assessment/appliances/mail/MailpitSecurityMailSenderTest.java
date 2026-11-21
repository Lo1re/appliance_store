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
