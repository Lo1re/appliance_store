package com.epam.rd.autocode.assessment.appliances.mail;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

class NotificationQueueFailureTest {
    @Test
    void rejectedQueueDoesNotPretendToDeliverTheNotification() {
        var sender = mock(SecurityMailSender.class);
        TaskExecutor executor =
                task -> {
                    throw new RejectedExecutionException();
                };
        new SecurityMailEventListener(sender, executor)
                .deliverNotification(
                        new SecurityNotificationMail(
                                "user@example.test",
                                SecurityNotificationMail.Kind.ACCOUNT_CREATED,
                                Locale.ENGLISH));
        verifyNoInteractions(sender);
    }
}
