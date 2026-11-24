package com.epam.rd.autocode.assessment.appliances.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator
        implements ConstraintValidator<PasswordsMatch, PasswordConfirmation> {
    @Override
    public boolean isValid(PasswordConfirmation value, ConstraintValidatorContext context) {
        if (value == null
                || value.getNewPassword() == null
                || value.getConfirmPassword() == null
                || value.getNewPassword().isBlank()
                || value.getConfirmPassword().isBlank()) {
            return true;
        }
        if (value.getNewPassword().equals(value.getConfirmPassword())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
