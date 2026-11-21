package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import java.net.URI;
import java.util.Locale;

public record PasswordResetMail(String recipient, URI link, Locale locale) {
    @Override public String toString() { return "PasswordResetMail[redacted]"; }
}
