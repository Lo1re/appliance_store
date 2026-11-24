package com.epam.rd.autocode.assessment.appliances.mail;

import java.util.Locale;

// Security metadata only, without passwords, tokens or provider credentials.
public record SecurityNotificationMail(String recipient, Kind kind, Locale locale) {
    public enum Kind {
        ACCOUNT_CREATED,
        PASSWORD_CHANGED
    }

    @Override
    public String toString() {
        return "SecurityNotificationMail[redacted]";
    }
}
