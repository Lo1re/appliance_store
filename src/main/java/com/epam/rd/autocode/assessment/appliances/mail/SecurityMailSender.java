package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import java.util.Locale;

public interface SecurityMailSender {
    void sendAccountCreated(String recipient, Locale locale);
    void sendPasswordChanged(String recipient, Locale locale);
    void sendEmailVerification(String recipient, URI link, Locale locale);
    void sendPasswordReset(String recipient, URI link, Locale locale);
}
