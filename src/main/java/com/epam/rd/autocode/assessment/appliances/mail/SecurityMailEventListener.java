package com.epam.rd.autocode.assessment.appliances.mail;

import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetMail;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "store.mail.enabled", havingValue = "true")
@Slf4j
public class SecurityMailEventListener {
    private final SecurityMailSender sender;
    private final TaskExecutor executor;

    public SecurityMailEventListener(SecurityMailSender sender, @Qualifier("securityMailExecutor") TaskExecutor executor) {
        this.sender = sender; this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(PasswordResetMail mail) {
        enqueue(() -> sender.sendPasswordReset(mail.recipient(), mail.link(), mail.locale()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliverNotification(SecurityNotificationMail mail) {
        enqueue(() -> {
            switch (mail.kind()) {
                case ACCOUNT_CREATED -> sender.sendAccountCreated(mail.recipient(), mail.locale());
                case PASSWORD_CHANGED -> sender.sendPasswordChanged(mail.recipient(), mail.locale());
            }
        });
    }

    @EventListener
    public void deliverVerification(EmailVerificationMail mail) {
        enqueue(() -> sender.sendEmailVerification(mail.recipient(), mail.link(), mail.locale()));
    }

    private void enqueue(Runnable delivery) {
        try {
            executor.execute(() -> {
                try { delivery.run(); log.info("Security email accepted by local mail server"); }
                catch (MailDeliveryException unavailable) { log.error("Security email delivery unavailable"); }
            });
        } catch (RejectedExecutionException unavailable) {
            log.error("Security email queue unavailable");
        }
    }
}
