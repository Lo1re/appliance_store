package com.epam.rd.autocode.assessment.appliances.security.jwt.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// No generated toString: the signing secret must never be logged.
@Getter
@Setter
@ConfigurationProperties("store.security.jwt")
public class JwtProperties {
    private String secretBase64;
    private String issuer = "appliance-store";
    private String audience = "appliance-store-api";
    private Duration ttl = Duration.ofMinutes(15);
    private Duration clockSkew = Duration.ofSeconds(30);
}
