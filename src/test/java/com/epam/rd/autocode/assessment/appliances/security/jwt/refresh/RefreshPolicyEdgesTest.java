package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class RefreshPolicyEdgesTest {
    void rejects(Duration ttl, Duration retention, long interval) {
        assertThatThrownBy(() -> new RefreshTokenProperties(ttl, retention, interval))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refreshPolicyRejectsMissingOrUnsafeBounds() {
        var day = Duration.ofDays(1);
        rejects(null, day, 1000);
        rejects(Duration.ZERO, day, 1000);
        rejects(Duration.ofDays(8), Duration.ofDays(9), 1000);
        rejects(day, null, 1000);
        rejects(day, Duration.ofHours(1), 1000);
        rejects(day, day, 999);
        assertThat(new RefreshTokenProperties(day, day, 1000).ttl()).isEqualTo(day);
    }
}
