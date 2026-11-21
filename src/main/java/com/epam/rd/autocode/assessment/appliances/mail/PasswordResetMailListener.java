package com.epam.rd.autocode.assessment.appliances.mail;

import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetMail;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "store.mail.enabled", havingValue = "true")
@Slf4j
public class PasswordResetMailListener {
    private final SecurityMailSender sender;
    private final TaskExecutor executor;

    public PasswordResetMailListener(SecurityMailSender sender, @Qualifier("securityMailExecutor") TaskExecutor executor) {
        this.sender = sender; this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(PasswordResetMail mail) {
        try {
            executor.execute(() -> send(mail));
        } catch (RejectedExecutionException unavailable) {
            log.error("Password reset email queue unavailable");
        }
    }

    private void send(PasswordResetMail mail) {
        try {
            sender.sendPasswordReset(mail.recipient(), mail.link(), mail.locale());
            log.info("Password reset email accepted by local mail server");
        } catch (MailDeliveryException unavailable) {
            log.error("Password reset email delivery unavailable");
        }
    }
}
