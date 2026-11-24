package com.epam.rd.autocode.assessment.appliances.security.login;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

class LoginAttemptPolicyTest {
    private final LoginAttemptPolicy policy =
            new LoginAttemptPolicy(
                    new LoginProtectionProperties(
                            5,
                            Duration.ofMinutes(15),
                            Duration.ofMinutes(15),
                            Duration.ofDays(1),
                            3600000));

    @Test
    void fifthFailureLocksWithoutExtendingTheDeadlineOnBlockedAttempts() {
        var now = Instant.parse("2026-01-01T00:00:00Z");
        var state = policy.failed("user@example.com", null, now);
        for (int i = 1; i < 5; i++) {
            state = policy.failed("user@example.com", state, now.plusSeconds(i));
        }
        assertThat(state.getFailedCount()).isEqualTo(5);
        assertThat(state.getLockedUntil())
                .isEqualTo(now.plusSeconds(4).plus(Duration.ofMinutes(15)));
        var lockedUntil = state.getLockedUntil();
        policy.failed("user@example.com", state, now.plusSeconds(10));
        assertThat(state.getLockedUntil()).isEqualTo(lockedUntil);
        assertThat(policy.locked(state, lockedUntil)).isFalse();
        assertThat(policy.failed("user@example.com", state, lockedUntil).getFailedCount())
                .isEqualTo(1);
    }

    @Test
    void expiredFailureWindowRestartsOnTheExistingEntity() {
        var now = Instant.parse("2026-01-01T00:00:00Z");
        var state = policy.failed("user@example.com", null, now);
        var reset = policy.failed("user@example.com", state, now.plus(Duration.ofMinutes(15)));
        assertThat(reset).isSameAs(state);
        assertThat(reset.getFailedCount()).isEqualTo(1);
        assertThat(reset.getFirstFailedAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }
}
