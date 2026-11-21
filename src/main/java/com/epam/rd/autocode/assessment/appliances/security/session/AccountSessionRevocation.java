package com.epam.rd.autocode.assessment.appliances.security.session;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 25)
public class AccountSessionRevocation {
    private final SessionRegistry sessions;

    @Around(value = "@annotation(com.epam.rd.autocode.assessment.appliances.security.session.RevokesAccountSessions) && args(accountId)",
            argNames = "operation,accountId")
    public Object afterDeletion(ProceedingJoinPoint operation, Long accountId) throws Throwable {
        Object result = operation.proceed();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { expire(accountId); }
            });
        } else {
            expire(accountId);
        }
        return result;
    }

    private void expire(Long accountId) {
        sessions.getAllPrincipals().stream()
                .filter(AccountPrincipal.class::isInstance).map(AccountPrincipal.class::cast)
                .filter(principal -> accountId.equals(principal.getAccountId()))
                .flatMap(principal -> sessions.getAllSessions(principal, false).stream())
                .forEach(session -> session.expireNow());
    }
}
