package com.epam.rd.autocode.assessment.appliances.model.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class PendingRegistrationTest {
    @Test
    void diagnosticTextNeverExposesRegistrationCredentials() {
        var pending =
                new PendingRegistration(
                        "secret-token",
                        "Private name",
                        "private@example.test",
                        "private-card",
                        "secret-password",
                        Instant.EPOCH);
        assertThat(pending.toString())
                .doesNotContain(
                        "secret-token",
                        "secret-password",
                        "private@example.test",
                        "private-card",
                        "Private name");
    }
}
