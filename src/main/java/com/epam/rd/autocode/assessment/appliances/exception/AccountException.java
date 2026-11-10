package com.epam.rd.autocode.assessment.appliances.exception;

public class AccountException extends RuntimeException {

    private final AccountError error;

    public AccountException(AccountError error) {
        super(error.messageKey());
        this.error = error;
    }

    public AccountError error() {
        return error;
    }
}