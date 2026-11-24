package com.epam.rd.autocode.assessment.appliances.security.registration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Profile("local & !prod")
@ConditionalOnExpression(
        "'${store.registration.enabled:false}' == 'true' && '${store.mail.enabled:false}' == 'true'"
                + " && '${store.security.auth-mode:database}' == 'database'")
public @interface ConditionalOnClientRegistration {}
