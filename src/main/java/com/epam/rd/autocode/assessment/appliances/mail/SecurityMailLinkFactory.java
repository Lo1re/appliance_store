package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Trusted local origin; never builds recovery links from the incoming Host header. */
@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "store.mail.enabled", havingValue = "true")
public class SecurityMailLinkFactory {
    private final String baseUrl;
    public SecurityMailLinkFactory(@Value("${store.mail.base-url}") String baseUrl) {
        URI origin = URI.create(baseUrl);
        if (!("http".equals(origin.getScheme()) || "https".equals(origin.getScheme())) || !("localhost".equals(origin.getHost()) || "127.0.0.1".equals(origin.getHost()))
                || origin.getRawQuery() != null || origin.getRawFragment() != null || origin.getUserInfo() != null
                || !(origin.getPath().isEmpty() || origin.getPath().equals("/")))
            throw new IllegalArgumentException("Local mail base URL must be a trusted loopback HTTP(S) origin");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    public URI registration(String token) { return URI.create(baseUrl + "/register/verify?token=" + token); }
    public URI passwordReset(String token) {
        return URI.create(baseUrl + "/password/reset?token=" + token);
    }
}
