package com.epam.rd.autocode.assessment.appliances.validation.password;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StrongPasswordValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepare() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void requiredValueBelongsOnlyToNotBlank(String value) {
        var violations = validator.validate(request(value));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessageTemplate())
                .isEqualTo("{jakarta.validation.constraints.NotBlank.message}");
    }

    @ParameterizedTest
    @CsvSource({
        "abcdefghijkl!1,uppercase",
        "ABCDEFGHIJKL!1,lowercase",
        "Abcdefghijkl!,digit",
        "Abcdefghijkl1,special"
    })
    void rejectsMissingCharacterCategories(String value, String category) {
        assertThat(validator.validate(request(value)))
                .extracting(v -> v.getMessageTemplate())
                .containsExactly("{user.password." + category + "}");
    }

    @Test
    void reportsEveryMissingCategoryTogether() {
        assertThat(validator.validate(request("abcdefghijkl")))
                .extracting(v -> v.getMessageTemplate())
                .containsExactlyInAnyOrder(
                        "{user.password.uppercase}",
                        "{user.password.digit}",
                        "{user.password.special}");
    }

    @Test
    void acceptsUnicodeCategoriesAndPreservesSpaces() {
        String password = "  Пароль١!тест  ";
        var request = request(password);
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getPassword()).isEqualTo(password);
    }

    @Test
    void enforcesLengthAndUtf8BoundariesOnThePasswordField() {
        assertThat(validator.validate(request("Aa1!" + "x".repeat(8)))).isEmpty();
        assertThat(validator.validate(request("Aa1!" + "x".repeat(7))))
                .extracting(v -> v.getMessageTemplate())
                .containsExactly("{user.password.too-short}");
        assertThat(validator.validate(request("Aa1!" + "я".repeat(34)))).isEmpty();
        var violations = validator.validate(request("Aa1!" + "я".repeat(34) + "x"));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("password");
        assertThat(violations.iterator().next().getMessageTemplate())
                .isEqualTo("{user.password.too-long}");
    }

    private PasswordRequest request(String value) {
        var request = new PasswordRequest();
        request.setPassword(value);
        return request;
    }
}
