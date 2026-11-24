package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import java.time.*;

class ExpiredLimiterWindowTest {
    @Test
    void expiredWindowsReleaseQuota() {
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH, Instant.EPOCH.plusSeconds(3601));
        var limiter = new ResetRequestLimiter(clock, new ResetTokenCodec(), 1, 1, 3600, 0);
        assertThat(limiter.acquire("email", "source")).isZero();
        assertThat(limiter.acquire("email", "source")).isZero();
    }
}
