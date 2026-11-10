package com.epam.rd.autocode.assessment.appliances.aspect;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class LoggingServices {

    @Around(
            value = "execution(public * com.epam.rd.autocode.assessment.appliances"
                    + ".service.impl..*.*(..)) && @annotation(transactional)",
            argNames = "operation,transactional"
    )
    public Object logServiceWrite(
            ProceedingJoinPoint operation,
            Transactional transactional
    ) throws Throwable {
        if (transactional.readOnly()) {
            return operation.proceed();
        }

        var signature = operation.getSignature();
        String operationName = signature.getDeclaringType().getSimpleName()
                + "." + signature.getName();
        long started = System.nanoTime();

        log.debug("Service operation started: {}", operationName);

        try {
            Object result = operation.proceed();

            log.info("Service operation completed: {} ({} ms)",
                    operationName, elapsedMillis(started));

            return result;
        } catch (Exception exception) {
            logFailure(operationName, elapsedMillis(started), exception);
            throw exception;
        }
    }

    private void logFailure(String operationName, long elapsedMillis, Exception exception) {
        String errorType = exception.getClass().getSimpleName();

        if (exception instanceof CatalogException
                || exception instanceof AccountException
                || exception instanceof OrderException
                || exception instanceof ConstraintViolationException) {
            log.warn("Service operation rejected: {} ({} ms, type={})",
                    operationName, elapsedMillis, errorType);
            return;
        }

        log.error("Service operation failed: {} ({} ms, type={})",
                operationName, elapsedMillis, errorType);
    }

    private long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}