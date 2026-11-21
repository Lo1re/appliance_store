package com.epam.rd.autocode.assessment.appliances.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private static final int MIN_LENGTH = 12;
    private static final int MAX_UTF8_BYTES = 72;
    private static final Pattern UPPERCASE = Pattern.compile("\\p{Lu}");
    private static final Pattern LOWERCASE = Pattern.compile("\\p{Ll}");
    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final Pattern SPECIAL = Pattern.compile("[\\p{P}\\p{S}]");

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
        valid &= require(value, UPPERCASE, context, "{user.password.uppercase}");
        valid &= require(value, LOWERCASE, context, "{user.password.lowercase}");
        valid &= require(value, DIGIT, context, "{user.password.digit}");
        valid &= require(value, SPECIAL, context, "{user.password.special}");
        return valid;
    }

    private boolean require(String value, Pattern category, ConstraintValidatorContext context, String message) {
        if (category.matcher(value).find()) {
            return true;
        }
        reject(context, message);
        return false;
    }

    private void reject(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
