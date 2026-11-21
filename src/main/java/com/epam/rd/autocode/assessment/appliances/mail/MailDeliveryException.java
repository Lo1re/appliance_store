package com.epam.rd.autocode.assessment.appliances.mail;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;

public class MailDeliveryException extends RuntimeException {
    public MailDeliveryException(Throwable cause) {
        super("Security email delivery unavailable", SafeFailure.snapshot(cause));
    }
}
