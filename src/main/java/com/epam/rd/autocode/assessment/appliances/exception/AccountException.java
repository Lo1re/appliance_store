package com.epam.rd.autocode.assessment.appliances.exception;

public class AccountException extends BusinessException {
    public AccountException(AccountError error) {
        super(error);
    }

    @Override
    public AccountError error() {
        return (AccountError) super.error();
    }
}
