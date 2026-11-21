package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("store.security.refresh")
public record RefreshTokenProperties(@DefaultValue("7d") Duration ttl, @DefaultValue("8d") Duration retention,
                                     @DefaultValue("3600000") long cleanupIntervalMs) {
    public RefreshTokenProperties {
        if (ttl == null || ttl.compareTo(Duration.ofMinutes(1)) < 0 || ttl.compareTo(Duration.ofDays(7)) > 0
                || retention == null || retention.compareTo(ttl) < 0 || cleanupIntervalMs < 1000) {
            throw new IllegalArgumentException("Invalid refresh-token time policy");
        }
    }
}
