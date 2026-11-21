package com.epam.rd.autocode.assessment.appliances.dto.manufacturer;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ManufacturerRequestTest {
    @Test
    void normalizesBeforeValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var manufacturer = new ManufacturerRequest();
            manufacturer.setName(null);
            assertThat(validator.validate(manufacturer)).hasSize(1);
            manufacturer.setName(" " + "A".repeat(150) + " ");
            assertThat(validator.validate(manufacturer)).isEmpty();
        }
    }
}
