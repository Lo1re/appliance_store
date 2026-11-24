package com.epam.rd.autocode.assessment.appliances.web.error;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;

import org.springframework.http.HttpStatus;

// One HTTP interpretation of domain error kinds, shared by HTML and JSON adapters.
public final class BusinessErrorStatus {
    private BusinessErrorStatus() {}

    public static HttpStatus of(BusinessException failure) {
        return switch (failure.error().kind()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case CONFLICT, BUSINESS_RULE -> HttpStatus.CONFLICT;
        };
    }
}
