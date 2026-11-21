package com.epam.rd.autocode.assessment.appliances.mail;

import java.net.URI;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "store.mail.enabled", havingValue = "true")
public class MailpitSecurityMailSender implements SecurityMailSender {
    private final JavaMailSender sender;
    private final MessageSource messages;
    private final String from;

    public MailpitSecurityMailSender(JavaMailSender sender, MessageSource messages,
            @Value("${store.mail.from}") String from) {
        this.sender = sender; this.messages = messages; this.from = from;
    }

    @Override
    public void sendPasswordReset(String recipient, URI link, Locale locale) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(messages.getMessage("mail.reset.subject", null, locale));
        message.setText(messages.getMessage("mail.reset.body", new Object[]{link.toASCIIString()}, locale));
        try {
            sender.send(message);
        } catch (MailException unavailable) {
            throw new MailDeliveryException(unavailable);
        }
    }
}
