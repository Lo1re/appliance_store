package com.epam.rd.autocode.assessment.appliances.security.password.reset;

public class InvalidPasswordResetException extends RuntimeException {
    public InvalidPasswordResetException() {
        super("Invalid or expired password reset link");
    }
}
