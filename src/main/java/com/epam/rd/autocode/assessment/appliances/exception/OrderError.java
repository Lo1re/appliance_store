package com.epam.rd.autocode.assessment.appliances.exception;

public enum OrderError implements ErrorCode {
    NOT_FOUND("order.not-found", ErrorKind.NOT_FOUND),
    NOT_EDITABLE("order.not-editable", ErrorKind.BUSINESS_RULE),
    ALREADY_APPROVED("order.already-approved", ErrorKind.BUSINESS_RULE),
    CLIENT_REQUIRED("order.client.required", ErrorKind.VALIDATION),
    CLIENT_NOT_FOUND("order.client.not-found", ErrorKind.VALIDATION),
    DUPLICATE_APPLIANCE("order.appliance.duplicate", ErrorKind.VALIDATION),
    APPLIANCE_NOT_FOUND("order.appliance.not-found", ErrorKind.VALIDATION);

    private final String messageKey;
    private final ErrorKind kind;

    OrderError(String messageKey, ErrorKind kind) {
        this.messageKey = messageKey;
        this.kind = kind;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
    @Override
    public ErrorKind kind() {
        return kind;
    }
}
