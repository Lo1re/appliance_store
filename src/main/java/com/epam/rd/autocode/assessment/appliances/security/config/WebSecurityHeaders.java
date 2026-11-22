package com.epam.rd.autocode.assessment.appliances.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.stereotype.Component;

/** HSTS is host-wide: local HTTP redirect/Mailpit ports must not be upgraded to TLS by the browser. */
@Component
public class WebSecurityHeaders implements Customizer<HeadersConfigurer<HttpSecurity>> {
    private final boolean hstsEnabled;
    public WebSecurityHeaders(@Value("${store.security.hsts.enabled:true}") boolean hstsEnabled) { this.hstsEnabled = hstsEnabled; }
    @Override public void customize(HeadersConfigurer<HttpSecurity> headers) {
        headers.httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(hstsEnabled ? 31536000 : 0).includeSubDomains(hstsEnabled));
    }
}
