package com.epam.rd.autocode.assessment.appliances.exception;

public class ManufacturerException extends RuntimeException {

    public ManufacturerException(String messageKey) {
        super(messageKey);
    }
}