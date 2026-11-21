package com.epam.rd.autocode.assessment.appliances.security.authentication;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("store.security")
public record AuthenticationProperties(@DefaultValue("database") Mode authMode) {
    public enum Mode { DATABASE, INMEMORY }
}
