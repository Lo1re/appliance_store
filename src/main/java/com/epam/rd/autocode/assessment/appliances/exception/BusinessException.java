package com.epam.rd.autocode.assessment.appliances.exception;

import java.util.Objects;

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode error;

    protected BusinessException(ErrorCode error) {
        super(Objects.requireNonNull(error).messageKey());
        this.error = error;
    }

    public ErrorCode error() {
        return error;
    }
}
