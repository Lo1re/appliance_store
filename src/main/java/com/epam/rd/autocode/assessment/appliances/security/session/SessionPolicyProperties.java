package com.epam.rd.autocode.assessment.appliances.security.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("store.security.sessions")
public record SessionPolicyProperties(@DefaultValue("2") @Min(1) @Max(10) int maximum) {}
