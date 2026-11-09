package com.epam.rd.autocode.assessment.appliances.exception;

public class ApplianceException extends RuntimeException {

    public ApplianceException(String messageKey) {
        super(messageKey);
    }
}