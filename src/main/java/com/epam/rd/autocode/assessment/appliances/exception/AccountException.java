package com.epam.rd.autocode.assessment.appliances.exception;

public class AccountException extends RuntimeException {

    public AccountException(String messageKey) {
        super(messageKey);
    }
}