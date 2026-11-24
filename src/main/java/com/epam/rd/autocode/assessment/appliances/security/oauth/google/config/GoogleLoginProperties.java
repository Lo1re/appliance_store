package com.epam.rd.autocode.assessment.appliances.security.oauth.google.config;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("store.security.google")
public class GoogleLoginProperties {
    @NotBlank private String clientId;
    @NotBlank private String clientSecret;
    @NotBlank private String redirectUri;

    @Override
    public String toString() {
        return "GoogleLoginProperties[redacted]";
    }
}
