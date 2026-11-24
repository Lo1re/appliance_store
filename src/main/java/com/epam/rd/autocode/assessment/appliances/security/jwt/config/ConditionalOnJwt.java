package com.epam.rd.autocode.assessment.appliances.security.jwt.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// JWT infrastructure is shared by the browser and the optional JSON API.
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression(
        "${store.api.enabled:false} or 'jwt'.equalsIgnoreCase('${store.security.web-auth:jwt}')")
public @interface ConditionalOnJwt {}
