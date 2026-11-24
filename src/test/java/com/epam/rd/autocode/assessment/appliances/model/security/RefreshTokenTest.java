package com.epam.rd.autocode.assessment.appliances.model.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class RefreshTokenTest {
    @Test
    void expiryBoundaryIsExclusiveAndRevocationRetainsItsFirstTimestamp() {
        Instant expiry = Instant.parse("2026-11-25T10:00:00Z");
        var token = new RefreshToken(7L, "fingerprint", expiry);
        assertThat(token.expiredAt(expiry.minusNanos(1))).isFalse();
        assertThat(token.expiredAt(expiry)).isTrue();
        assertThat(token.expiredAt(expiry.plusNanos(1))).isTrue();
        token.revoke(expiry.minusSeconds(2));
        token.revoke(expiry.minusSeconds(1));
        assertThat(token.getRevokedAt()).isEqualTo(expiry.minusSeconds(2));
        token.replace(expiry, 8L);
        assertThat(token.getReplacedBy()).isEqualTo(8L);
        assertThat(token.getRevokedAt()).isEqualTo(expiry.minusSeconds(2));
    }
}
