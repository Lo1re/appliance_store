package com.epam.rd.autocode.assessment.appliances.exception;

public enum CatalogError implements ErrorCode {
    MANUFACTURER_NOT_FOUND("manufacturer.not-found"),
    MANUFACTURER_DUPLICATE_NAME("manufacturer.name.duplicate"),
    MANUFACTURER_IN_USE("manufacturer.in-use"),
    APPLIANCE_NOT_FOUND("appliance.not-found"),
    APPLIANCE_MANUFACTURER_NOT_FOUND("appliance.manufacturer.not-found"),
    APPLIANCE_IN_USE("appliance.in-use");

    private final String messageKey;

    CatalogError(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}