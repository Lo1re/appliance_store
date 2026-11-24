package com.epam.rd.autocode.assessment.appliances.aspect;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.logging.OperationCompletionLogger;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CompletionSynchronizationEdgesTest {
    @Test
    void activeButUnsynchronizedTransactionsDoNotRegisterCallbacks() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            new OperationCompletionLogger().afterCommit("operation", 1);
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
