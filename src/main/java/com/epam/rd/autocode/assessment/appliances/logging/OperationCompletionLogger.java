package com.epam.rd.autocode.assessment.appliances.logging;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class OperationCompletionLogger {
    public void afterCommit(String name, long elapsedMillis) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            completed(name, elapsedMillis);
                        }
                    });
        } else {
            completed(name, elapsedMillis);
        }
    }

    private void completed(String name, long elapsedMillis) {
        log.info("Service operation completed: {} ({} ms)", name, elapsedMillis);
    }
}
