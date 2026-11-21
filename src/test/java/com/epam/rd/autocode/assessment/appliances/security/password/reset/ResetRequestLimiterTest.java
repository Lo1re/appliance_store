package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResetRequestLimiterTest {
    @Test void limitsKnownAndUnknownEmailsAndBoundsSourceAcrossEmails() {
        var codec = new ResetTokenCodec();
        var limits = new ResetRequestLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), codec, 5, 20, 3600, 0);
        for (int i = 0; i < 5; i++) assertThat(limits.acquire("unknown@example.test", "source")).isZero();
        assertThat(limits.acquire("unknown@example.test", "another-source")).isPositive();
        for (int i = 0; i < 15; i++) assertThat(limits.acquire("email" + i, "source")).isZero();
        assertThat(limits.acquire("new-email", "source")).isPositive();
    }
    @Test void suppressesDuplicateRequestsUntilCooldownEnds() {
        var clock = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH);
        var limits = new ResetRequestLimiter(clock, new ResetTokenCodec(), 5, 20, 3600, 60);
        assertThat(limits.acquire("user@example.test", "source")).isZero();
        assertThat(limits.acquire("user@example.test", "source")).isPositive();
        assertThat(limits.acquire("user@example.test", "other-source")).isEqualTo(60);
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(59).plusMillis(100));
        assertThat(limits.acquire("user@example.test", "source")).isEqualTo(1);
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(60));
        assertThat(limits.acquire("user@example.test", "source")).isZero();
    }

    @Test void cooldownSpamDoesNotConsumeHourlyEmailOrSourceQuota() {
        var clock = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH);
        var limits = new ResetRequestLimiter(clock, new ResetTokenCodec(), 5, 2, 3600, 60);
        assertThat(limits.acquire("user@example.test", "source")).isZero();
        for (int i = 0; i < 100; i++) {
            assertThat(limits.acquire("user@example.test", "source")).isEqualTo(60);
        }
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(60));
        assertThat(limits.acquire("user@example.test", "source")).isZero();
        org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(120));
        assertThat(limits.acquire("another@example.test", "source")).isEqualTo(3480);
    }

    @Test void rejectedEmailQuotaRequestsDoNotConsumeAnotherSourcesBudget() {
        var limits = new ResetRequestLimiter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), new ResetTokenCodec(), 1, 2, 3600, 0);
        assertThat(limits.acquire("limited@example.test", "first-source")).isZero();
        for (int i = 0; i < 100; i++) {
            assertThat(limits.acquire("limited@example.test", "second-source")).isEqualTo(3600);
        }
        assertThat(limits.acquire("another@example.test", "second-source")).isZero();
        assertThat(limits.acquire("third@example.test", "second-source")).isZero();
        assertThat(limits.acquire("fourth@example.test", "second-source")).isEqualTo(3600);
    }

    @Test void tokensAreCanonicalRandomAndContainNoStoredRawValue() {
        var codec = new ResetTokenCodec();
        String token = codec.generate();
        assertThat(token).hasSize(43).isNotEqualTo(codec.generate());
        assertThat(codec.valid(token)).isTrue();
        assertThat(codec.valid("invalid")).isFalse();
        assertThat(codec.valid(null)).isFalse();
        assertThat(codec.hash(token)).hasSize(64).isNotEqualTo(token);
    }
}
