package com.epam.rd.autocode.assessment.appliances.security.logging;

import com.epam.rd.autocode.assessment.appliances.security.password.PasswordChanged;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PasswordChangeEventLogger {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void changed(PasswordChanged event) {
        log.info("Password changed; refresh access revoked (accountId={})", event.accountId());
    }
}
