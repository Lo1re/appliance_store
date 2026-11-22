package com.epam.rd.autocode.assessment.appliances.mail;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
class DeliveryFailureIsolationTest {@Test void localMailFailureIsHandledByTheBackgroundDeliveryBoundary(){var sender=mock(SecurityMailSender.class);doThrow(new MailDeliveryException(new IllegalStateException("private"))).when(sender).sendAccountCreated(any(),any());new SecurityMailEventListener(sender,Runnable::run).deliverNotification(new SecurityNotificationMail("user@example.test",SecurityNotificationMail.Kind.ACCOUNT_CREATED,Locale.ENGLISH));verify(sender).sendAccountCreated("user@example.test",Locale.ENGLISH);}}
