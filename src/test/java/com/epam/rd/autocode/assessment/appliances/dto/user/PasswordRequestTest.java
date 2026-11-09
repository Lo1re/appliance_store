package com.epam.rd.autocode.assessment.appliances.dto.user;

import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
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
            assertThat(validator.validate(request)).hasSize(1);
            String accepted = " A1!" + "я".repeat(34);
            request.setPassword(accepted);
            assertThat(request.getPassword()).isEqualTo(accepted);
            assertThat(validator.validate(request)).isEmpty();
            request.setPassword(accepted + "я");
            assertThat(validator.validate(request))
                    .extracting(v -> v.getMessageTemplate())
                    .containsExactly("{user.password.too-long}");
        }
    }
}
