package com.epam.rd.autocode.assessment.appliances.exception;

public class CatalogException extends RuntimeException {

    private final CatalogError error;

    public CatalogException(CatalogError error) {
        super(error.messageKey());
        this.error = error;
    }

    public CatalogError error() {
        return error;
    }
}