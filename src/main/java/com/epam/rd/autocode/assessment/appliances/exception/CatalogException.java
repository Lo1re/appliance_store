package com.epam.rd.autocode.assessment.appliances.exception;

public class CatalogException extends BusinessException {
    public CatalogException(CatalogError error) {
        super(error);
    }

    @Override
    public CatalogError error() {
        return (CatalogError) super.error();
    }
}
