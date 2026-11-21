package com.epam.rd.autocode.assessment.appliances.dto.user;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordRequestTest {
    @Test
    void passwordUsesUtf8ByteLimitAndPreservesSpaces() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new PasswordRequest();
            request.setPassword(null);
            assertThat(validator.validate(request)).hasSize(1);
            request.setPassword("longbutonlylowercase");
            assertThat(validator.validate(request))
                    .extracting(violation -> violation.getMessageTemplate())
                    .containsExactlyInAnyOrder(
                            "{user.password.uppercase}",
                            "{user.password.digit}",
                            "{user.password.special}");
            String accepted = " A1!" + "я".repeat(34);
            request.setPassword(accepted);
            assertThat(request.getPassword()).isEqualTo(accepted);
            assertThat(validator.validate(request)).isEmpty();
            request.setPassword(accepted + "я");
            assertThat(validator.validate(request))
                    .allSatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("password"))
                    .extracting(v -> v.getMessageTemplate())
                    .containsExactly("{user.password.too-long}");
        }
    }
}
