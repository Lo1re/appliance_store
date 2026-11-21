package com.epam.rd.autocode.assessment.appliances.exception;

public enum AccountError implements ErrorCode {

    CARD_DUPLICATE("client.card.duplicate", ErrorKind.CONFLICT),
    EMAIL_DUPLICATE("user.email.duplicate", ErrorKind.CONFLICT),
    CLIENT_NOT_FOUND("client.not-found", ErrorKind.NOT_FOUND),
    CLIENT_IN_USE("client.in-use", ErrorKind.CONFLICT),
    EMPLOYEE_NOT_FOUND("employee.not-found", ErrorKind.NOT_FOUND),
    EMPLOYEE_IN_USE("employee.in-use", ErrorKind.CONFLICT),
    EMPLOYEE_SELF_DELETE("employee.self-delete", ErrorKind.CONFLICT),
    LAST_EMPLOYEE("employee.last-employee", ErrorKind.CONFLICT);

    private final String messageKey;
    private final ErrorKind kind;

    AccountError(String messageKey, ErrorKind kind) {
        this.messageKey = messageKey;
        this.kind = kind;
    }

    public String messageKey() {
        return messageKey;
    }
    @Override
    public ErrorKind kind() {
        return kind;
    }
}
