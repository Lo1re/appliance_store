package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import java.util.Locale;

public interface SecurityMailSender {
    void sendPasswordReset(String recipient, URI link, Locale locale);
}
