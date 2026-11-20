package com.epam.rd.autocode.assessment.appliances.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private static final int MIN_LENGTH = 12;
    private static final int MAX_UTF8_BYTES = 72;
    private static final Pattern COMPLEXITY = Pattern.compile(
            "(?s)^(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\p{Nd})(?=.*[\\p{P}\\p{S}]).*$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        boolean valid = true;
        context.disableDefaultConstraintViolation();
        if (value.length() < MIN_LENGTH) {
            reject(context, "{user.password.too-short}");
            valid = false;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            reject(context, "{user.password.too-long}");
            valid = false;
        }
        if (!COMPLEXITY.matcher(value).matches()) {
            reject(context, "{user.password.complexity}");
            valid = false;
        }
        return valid;
    }

    private void reject(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
