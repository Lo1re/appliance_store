package com.epam.rd.autocode.assessment.appliances.validation.password;

// Typed validation contract; no reflective access to arbitrary DTO properties.
public interface PasswordConfirmation {
    String getNewPassword();

    String getConfirmPassword();
}
