package com.epam.rd.autocode.assessment.appliances.security.token;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class OpaqueTokensEdgesTest {
    @Test
    void canonicalEncodingRejectsWrongPadBitsAndMalformedInput() {
        String token = OpaqueTokens.generate();
        assertThat(OpaqueTokens.isCanonical(token)).isTrue();
        assertThat(OpaqueTokens.isCanonical(null)).isFalse();
        assertThat(OpaqueTokens.isCanonical("bad")).isFalse();
        assertThat(OpaqueTokens.isCanonical("A".repeat(42) + "B")).isFalse();
    }

    @Test
    void unavailableDigestProviderFailsClosed() {
        OpaqueTokens.generate();
        try (var digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException());
            assertThatThrownBy(() -> OpaqueTokens.fingerprint("token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("SHA-256 unavailable");
        }
    }
}
