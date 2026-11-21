package com.epam.rd.autocode.assessment.appliances.exception.password;

import com.epam.rd.autocode.assessment.appliances.exception.ErrorCode;
import com.epam.rd.autocode.assessment.appliances.exception.ErrorKind;

public enum PasswordChangeError implements ErrorCode {
    NOT_SET("password.setup.first"), CURRENT_INVALID("password.current.invalid"), NEW_UNCHANGED("password.new.unchanged");
    private final String key;
    PasswordChangeError(String key) { this.key = key; }
    @Override public String messageKey() { return key; }
    @Override public ErrorKind kind() { return ErrorKind.VALIDATION; }
}
