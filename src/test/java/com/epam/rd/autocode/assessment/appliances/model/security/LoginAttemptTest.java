package com.epam.rd.autocode.assessment.appliances.model.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class LoginAttemptTest {
    @Test
    void persistenceIdentityMatchesNormalizedAccountEmail() {
        var attempt = new LoginAttempt("client@example.test", 1, Instant.EPOCH, null);
        assertThat(attempt.getId()).isEqualTo(attempt.getEmail());
        assertThat(attempt.isNew()).isTrue();
    }
}
