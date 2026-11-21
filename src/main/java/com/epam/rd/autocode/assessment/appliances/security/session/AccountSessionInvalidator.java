package com.epam.rd.autocode.assessment.appliances.security.session;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Session expiration only; the caller/event determines the successful-commit boundary. */
@Component
@RequiredArgsConstructor
public class AccountSessionInvalidator {
    private final SessionRegistry sessions;
    public void expire(Long accountId) {
        sessions.getAllPrincipals().stream().filter(AccountPrincipal.class::isInstance).map(AccountPrincipal.class::cast)
                .filter(principal -> accountId.equals(principal.getAccountId()))
                .flatMap(principal -> sessions.getAllSessions(principal, false).stream()).forEach(session -> session.expireNow());
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordChanged(PasswordChanged event) { expire(event.accountId()); }
}
