package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Shared browser-cookie security, independent of the JSON API transport selection. */
@ConfigurationProperties("store.security.cookies")
public record AuthCookieProperties(@DefaultValue("true") boolean secure) { }
