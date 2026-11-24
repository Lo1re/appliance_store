package com.epam.rd.autocode.assessment.appliances.aspect;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.logging.OperationCompletionLogger;

import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 30)
public class LoggingServices {
    private final OperationCompletionLogger completionLogger;

    @Around("@annotation(com.epam.rd.autocode.assessment.appliances.aspect.Loggable)")
    public Object logOperation(ProceedingJoinPoint operation) throws Throwable {
        var signature = operation.getSignature();
        String name = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        long started = System.nanoTime();
        log.debug("Service operation started: {}", name);
        try {
            Object result = operation.proceed();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            log.debug("Service invocation returned: {} ({} ms)", name, elapsed);
            completionLogger.afterCommit(name, elapsed);
            return result;
        } catch (Exception exception) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            String type = exception.getClass().getSimpleName();
            if (exception instanceof BusinessException
                    || exception instanceof ConstraintViolationException
                    || exception instanceof AccessDeniedException) {
                log.warn("Service operation rejected: {} ({} ms, type={})", name, elapsed, type);
            } else {
                log.error("Service operation failed: {} ({} ms, type={})", name, elapsed, type);
            }
            throw exception;
        }
    }
}
