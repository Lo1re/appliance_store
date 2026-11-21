package com.epam.rd.autocode.assessment.appliances.security.session;

import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountSessionRevocationTest {
    @Test
    void expiresOnlyTargetSessionsAfterSuccessfulCompletion() throws Throwable {
        var registry = new SessionRegistryImpl();
        registry.registerNewSession("target", new StorePrincipal(2L, "target@example.com", "hash", "EMPLOYEE"));
        registry.registerNewSession("other", new StorePrincipal(3L, "other@example.com", "hash", "EMPLOYEE"));
        new AccountSessionRevocation(registry).afterDeletion(mock(ProceedingJoinPoint.class), 2L);
        assertThat(registry.getSessionInformation("target").isExpired()).isTrue();
        assertThat(registry.getSessionInformation("other").isExpired()).isFalse();
    }

    @Test
    void defersExpirationToOuterCommitAndKeepsSessionsOnRollback() throws Throwable {
        var registry = new SessionRegistryImpl();
        registry.registerNewSession("target", new StorePrincipal(2L, "target@example.com", "hash", "EMPLOYEE"));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            new AccountSessionRevocation(registry).afterDeletion(mock(ProceedingJoinPoint.class), 2L);
            assertThat(registry.getSessionInformation("target").isExpired()).isFalse();
            var hooks = TransactionSynchronizationManager.getSynchronizations();
            hooks.forEach(hook -> hook.afterCompletion(1));
            assertThat(registry.getSessionInformation("target").isExpired()).isFalse();
            hooks.forEach(hook -> hook.afterCommit());
            assertThat(registry.getSessionInformation("target").isExpired()).isTrue();
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void failedDeletionDoesNotExpireSessions() throws Throwable {
        var registry = new SessionRegistryImpl();
        registry.registerNewSession("target", new StorePrincipal(2L, "target@example.com", "hash", "EMPLOYEE"));
        var operation = mock(ProceedingJoinPoint.class);
        var failure = new IllegalStateException("failed");
        when(operation.proceed()).thenThrow(failure);
        assertThatThrownBy(() -> new AccountSessionRevocation(registry).afterDeletion(operation, 2L)).isSameAs(failure);
        assertThat(registry.getSessionInformation("target").isExpired()).isFalse();
    }
}
