package com.epam.rd.autocode.assessment.appliances.security.login;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("store.security.login")
public record LoginProtectionProperties(
        @DefaultValue("5") @Min(1) @Max(100) int maxFailures,
        @DefaultValue("15m") Duration window,
        @DefaultValue("15m") Duration lockDuration,
        @DefaultValue("24h") Duration retention,
        @DefaultValue("3600000") @Min(1000) long cleanupIntervalMs) {
    public LoginProtectionProperties {
        if (window == null
                || window.isZero()
                || window.isNegative()
                || lockDuration == null
                || lockDuration.isZero()
                || lockDuration.isNegative()
                || retention == null
                || retention.compareTo(window) < 0
                || retention.compareTo(lockDuration) < 0) {
            throw new IllegalArgumentException(
                    "Login protection durations must be positive and retention must cover both"
                            + " windows");
        }
    }
}
