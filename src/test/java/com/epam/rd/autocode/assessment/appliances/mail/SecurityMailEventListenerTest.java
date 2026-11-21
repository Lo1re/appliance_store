package com.epam.rd.autocode.assessment.appliances.mail;

import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetMail;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class SecurityMailEventListenerTest {
    @Test void registrationUsesBackgroundDeliveryAndItsOwnTemplate() {
        var sender = mock(SecurityMailSender.class); var executor = mock(TaskExecutor.class);
        var listener = new SecurityMailEventListener(sender, executor);
        var event = new RegistrationVerificationMail("client@example.test", URI.create("http://localhost:8080/register/verify?token=secret"), Locale.ENGLISH);
        listener.deliverRegistration(event); verifyNoInteractions(sender);
        var job = ArgumentCaptor.forClass(Runnable.class); verify(executor).execute(job.capture()); job.getValue().run();
        verify(sender).sendRegistrationVerification(event.recipient(), event.link(), event.locale());
    }

    @Test void queuesDeliveryWithoutBlockingTheCallingThread() {
        var sender = mock(SecurityMailSender.class);
        var executor = mock(TaskExecutor.class);
        var listener = new SecurityMailEventListener(sender, executor);
        var event = new PasswordResetMail("test@example.test", URI.create("http://localhost:8080/password/reset?token=secret"), Locale.ENGLISH);
        listener.deliver(event);
        verifyNoInteractions(sender);
        var job = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(job.capture());
        job.getValue().run();
        verify(sender).sendPasswordReset(event.recipient(), event.link(), event.locale());
    }

    @Test void verificationUsesTheSameBackgroundDeliveryQueue() {
        var sender = mock(SecurityMailSender.class); var executor = mock(TaskExecutor.class);
        var listener = new SecurityMailEventListener(sender, executor);
        var event = new EmailVerificationMail("test@example.test", URI.create("http://localhost:8080/onboarding/google/verify?token=secret"), Locale.ENGLISH);
        listener.deliverVerification(event); verifyNoInteractions(sender);
        var job = ArgumentCaptor.forClass(Runnable.class); verify(executor).execute(job.capture()); job.getValue().run();
        verify(sender).sendEmailVerification(event.recipient(), event.link(), event.locale());
    }

    @Test void notificationEventsUseTheBackgroundQueueWithoutSecrets() {
        var sender = mock(SecurityMailSender.class);
        var listener = new SecurityMailEventListener(sender, Runnable::run);
        listener.deliverNotification(new SecurityNotificationMail("test@example.test", SecurityNotificationMail.Kind.ACCOUNT_CREATED, Locale.ENGLISH));
        listener.deliverNotification(new SecurityNotificationMail("test@example.test", SecurityNotificationMail.Kind.PASSWORD_CHANGED, Locale.ENGLISH));
        verify(sender).sendAccountCreated("test@example.test", Locale.ENGLISH);
        verify(sender).sendPasswordChanged("test@example.test", Locale.ENGLISH);
    }

    @Test void queueRejectionDoesNotExposeTheAccountOrFailTheHttpRequest() {
        var executor = mock(TaskExecutor.class);
        doThrow(new RejectedExecutionException()).when(executor).execute(any(Runnable.class));
        var listener = new SecurityMailEventListener(mock(SecurityMailSender.class), executor);
        assertThatCode(() -> listener.deliver(new PasswordResetMail("test@example.test", URI.create("http://localhost:8080"), Locale.ENGLISH)))
                .doesNotThrowAnyException();
    }
}
