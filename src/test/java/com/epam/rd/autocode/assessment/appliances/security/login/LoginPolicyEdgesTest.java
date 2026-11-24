package com.epam.rd.autocode.assessment.appliances.security.login;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class LoginPolicyEdgesTest {
    void rejects(Duration window, Duration lock, Duration retention) {
        assertThatThrownBy(() -> new LoginProtectionProperties(5, window, lock, retention, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void durationPolicyFailsClosedForEachInvalidInput() {
        var day = Duration.ofDays(1);
        var minute = Duration.ofMinutes(1);
        rejects(null, minute, day);
        rejects(Duration.ZERO, minute, day);
        rejects(Duration.ofSeconds(-1), minute, day);
        rejects(minute, null, day);
        rejects(minute, Duration.ZERO, day);
        rejects(minute, Duration.ofSeconds(-1), day);
        rejects(minute, minute, null);
        rejects(day, minute, minute);
        rejects(minute, day, minute);
        assertThat(new LoginProtectionProperties(5, minute, minute, day, 1000).maxFailures())
                .isEqualTo(5);
    }
}
