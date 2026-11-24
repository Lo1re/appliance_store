package com.epam.rd.autocode.assessment.appliances.exception;

public enum CatalogError implements ErrorCode {
    MANUFACTURER_NOT_FOUND("manufacturer.not-found", ErrorKind.NOT_FOUND),
    MANUFACTURER_DUPLICATE_NAME("manufacturer.name.duplicate", ErrorKind.CONFLICT),
    MANUFACTURER_IN_USE("manufacturer.in-use", ErrorKind.CONFLICT),
    APPLIANCE_NOT_FOUND("appliance.not-found", ErrorKind.NOT_FOUND),
    APPLIANCE_MANUFACTURER_NOT_FOUND("appliance.manufacturer.not-found", ErrorKind.VALIDATION),
    APPLIANCE_IN_USE("appliance.in-use", ErrorKind.CONFLICT);

    private final String messageKey;
    private final ErrorKind kind;

    CatalogError(String messageKey, ErrorKind kind) {
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
