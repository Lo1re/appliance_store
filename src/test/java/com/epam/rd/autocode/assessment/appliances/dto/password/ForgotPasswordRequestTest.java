package com.epam.rd.autocode.assessment.appliances.dto.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ForgotPasswordRequestTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidation() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidation() {
        factory.close();
    }

    @Test
    void canonicalizesEmailBeforeValidation() {
        var request = new ForgotPasswordRequest();
        request.setEmail("  Mercury@EXAMPLE.COM  ");

        assertEquals("mercury@example.com", request.getEmail());
        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void normalizationDoesNotMakeMissingEmailValid(String email) {
        var request = new ForgotPasswordRequest();
        request.setEmail(email);

        assertFalse(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "person@", "@example.com"})
    void stillRejectsMalformedEmail(String email) {
        var request = new ForgotPasswordRequest();
        request.setEmail(email);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void stillRejectsEmailExceedingTheStorageContract() {
        var request = new ForgotPasswordRequest();
        request.setEmail("a".repeat(245) + "@example.com");

        assertFalse(validator.validate(request).isEmpty());
    }
}
