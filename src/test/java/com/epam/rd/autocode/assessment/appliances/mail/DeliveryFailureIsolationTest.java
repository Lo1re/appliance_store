package com.epam.rd.autocode.assessment.appliances.mail;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import java.util.Locale;

class DeliveryFailureIsolationTest {
    @Test
    void localMailFailureIsHandledByTheBackgroundDeliveryBoundary() {
        var sender = mock(SecurityMailSender.class);
        doThrow(new MailDeliveryException(new IllegalStateException("private")))
                .when(sender)
                .sendAccountCreated(any(), any());
        new SecurityMailEventListener(sender, Runnable::run)
                .deliverNotification(
                        new SecurityNotificationMail(
                                "user@example.test",
                                SecurityNotificationMail.Kind.ACCOUNT_CREATED,
                                Locale.ENGLISH));
        verify(sender).sendAccountCreated("user@example.test", Locale.ENGLISH);
    }
}
