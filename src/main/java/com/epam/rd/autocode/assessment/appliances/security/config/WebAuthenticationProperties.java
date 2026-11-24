package com.epam.rd.autocode.assessment.appliances.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// The session mode is explicit; ordinary browser launches use JWT.
@ConfigurationProperties("store.security")
public record WebAuthenticationProperties(@DefaultValue("jwt") Mode webAuth) {
    public enum Mode {
        JWT,
        SESSION
    }
}
