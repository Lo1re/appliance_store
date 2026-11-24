package com.epam.rd.autocode.assessment.appliances.security.oauth.google.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "store.security.google.enabled", havingValue = "true")
public @interface ConditionalOnGoogleLogin {}
