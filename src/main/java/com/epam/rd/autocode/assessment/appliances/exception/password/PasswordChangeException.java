package com.epam.rd.autocode.assessment.appliances.exception.password;

import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;

public class PasswordChangeException extends BusinessException {
    public PasswordChangeException(PasswordChangeError error) {
        super(error);
    }

    @Override
    public PasswordChangeError error() {
        return (PasswordChangeError) super.error();
    }
}
