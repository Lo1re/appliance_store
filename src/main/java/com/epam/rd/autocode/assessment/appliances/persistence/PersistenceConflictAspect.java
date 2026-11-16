package com.epam.rd.autocode.assessment.appliances.persistence;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class PersistenceConflictAspect {
    private final PersistenceConflictTranslator translator;

    @Around(value = "@annotation(boundary)", argNames = "operation,boundary")
    public Object translate(ProceedingJoinPoint operation, PersistenceBoundary boundary) throws Throwable {
        try {
            return operation.proceed();
        } catch (RuntimeException failure) {
            // A joined outer transaction still owns rollback; preserve its original failure.
            if (failure instanceof BusinessException
                    || TransactionSynchronizationManager.isActualTransactionActive()) {
                throw failure;
            }
            throw translator.translate(boundary.value(), failure);
        }
    }
}
