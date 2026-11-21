package com.epam.rd.autocode.assessment.appliances.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("store.api")
public record ApiTransportProperties(@DefaultValue("cookie") Transport tokenTransport) {
    public enum Transport { HEADER, COOKIE }
    public boolean cookieMode() { return tokenTransport == Transport.COOKIE; }
}
