package com.epam.rd.autocode.assessment.appliances.validation.password;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PasswordConfirmationValidationTest {
    @Test void mismatchesBelongToTheConfirmationFieldWithoutExposingPasswords() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var form = new PasswordChangeRequest();
            form.setCurrentPassword("OldReviewPass2026!"); form.setNewPassword("NewReviewPass2026!"); form.setConfirmPassword("DifferentReviewPass2026!");
            var violations = factory.getValidator().validate(form);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confirmPassword")
                    && v.getMessageTemplate().equals("{password.confirm.mismatch}"));
            assertThat(form.toString()).doesNotContain(form.getCurrentPassword(), form.getNewPassword(), form.getConfirmPassword());
            form.setConfirmPassword(form.getNewPassword()); assertThat(factory.getValidator().validate(form)).isEmpty();
            form.setNewPassword("weak"); form.setConfirmPassword("weak");
            assertThat(factory.getValidator().validate(form)).anyMatch(v -> v.getPropertyPath().toString().equals("newPassword"));
        }
    }
}
