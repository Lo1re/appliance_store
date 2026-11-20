package com.epam.rd.autocode.assessment.appliances.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Enforces account validity for every public application-service entry point. */
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 15)
public class ActiveAccountGuard {
    private final CurrentActor currentActor;

    @Before("execution(public * com.epam.rd.autocode.assessment.appliances.service..*.*(..)) && @within(org.springframework.stereotype.Service)")
    public void requireActiveAccount() {
        currentActor.requireUser();
    }
}
