package com.epam.rd.autocode.assessment.appliances.exception;

public enum AccountError {

    EMAIL_DUPLICATE("user.email.duplicate"),
    CLIENT_NOT_FOUND("client.not-found"),
    CLIENT_IN_USE("client.in-use"),
    EMPLOYEE_NOT_FOUND("employee.not-found"),
    EMPLOYEE_IN_USE("employee.in-use");

    private final String messageKey;

    AccountError(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}