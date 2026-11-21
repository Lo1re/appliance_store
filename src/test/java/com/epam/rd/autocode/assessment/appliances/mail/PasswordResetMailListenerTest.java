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

class PasswordResetMailListenerTest {
    @Test void queuesDeliveryWithoutBlockingTheCallingThread() {
        var sender = mock(SecurityMailSender.class);
        var executor = mock(TaskExecutor.class);
        var listener = new PasswordResetMailListener(sender, executor);
        var event = new PasswordResetMail("test@example.test", URI.create("http://localhost:8080/password/reset?token=secret"), Locale.ENGLISH);
        listener.deliver(event);
        verifyNoInteractions(sender);
        var job = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(job.capture());
        job.getValue().run();
        verify(sender).sendPasswordReset(event.recipient(), event.link(), event.locale());
    }

    @Test void queueRejectionDoesNotExposeTheAccountOrFailTheHttpRequest() {
        var executor = mock(TaskExecutor.class);
        doThrow(new RejectedExecutionException()).when(executor).execute(any(Runnable.class));
        var listener = new PasswordResetMailListener(mock(SecurityMailSender.class), executor);
        assertThatCode(() -> listener.deliver(new PasswordResetMail("test@example.test", URI.create("http://localhost:8080"), Locale.ENGLISH)))
                .doesNotThrowAnyException();
    }
}
