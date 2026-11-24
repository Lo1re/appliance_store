package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import java.util.Locale;

public record RegistrationVerificationMail(String recipient, URI link, Locale locale) {
    @Override
    public String toString() {
        return "RegistrationVerificationMail[redacted]";
    }
}
