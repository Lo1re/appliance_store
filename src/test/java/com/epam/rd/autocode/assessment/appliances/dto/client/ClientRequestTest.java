package com.epam.rd.autocode.assessment.appliances.dto.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class ClientRequestTest {
    @Test
    void normalizesBeforeValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var client = new ClientRequest();
            client.setName(null);
            client.setEmail(null);
            client.setCard(null);
            assertThat(validator.validate(client)).hasSize(2);
            client.setName(" Buyer ");
            client.setEmail(" BUYER@Example.com ");
            client.setCard(" DEMO ");
            assertThat(client.getName()).isEqualTo("Buyer");
            assertThat(client.getEmail()).isEqualTo("buyer@example.com");
            assertThat(client.getCard()).isEqualTo("DEMO");
            assertThat(validator.validate(client)).isEmpty();
            client.setCard("   ");
            assertThat(client.getCard()).isNull();
            assertThat(validator.validate(client)).isEmpty();
            client.setEmail("not-an-email");
            assertThat(validator.validate(client)).hasSize(1);
        }
    }
}
