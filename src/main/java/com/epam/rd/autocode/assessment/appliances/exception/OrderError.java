package com.epam.rd.autocode.assessment.appliances.exception;

public enum OrderError implements ErrorCode {
    NOT_FOUND("order.not-found"),
    NOT_EDITABLE("order.not-editable"),
    ALREADY_APPROVED("order.already-approved"),
    CLIENT_REQUIRED("order.client.required"),
    CLIENT_NOT_FOUND("order.client.not-found"),
    DUPLICATE_APPLIANCE("order.appliance.duplicate"),
    APPLIANCE_NOT_FOUND("order.appliance.not-found");

    private final String messageKey;

    OrderError(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
