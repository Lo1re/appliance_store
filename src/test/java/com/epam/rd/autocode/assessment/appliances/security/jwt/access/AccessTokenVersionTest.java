package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.*;

class AccessTokenVersionTest {
    @Test void acceptsLegacyZeroAndIntegerVersions() {
        assertThat(AccessTokenVersion.read(Jwt.withTokenValue("test").header("alg", "HS256").subject("1").build())).isZero();
        assertThat(AccessTokenVersion.read(token(3L))).isEqualTo(3);
    }
    @ParameterizedTest @MethodSource("invalid")
    void rejectsMalformedVersionClaims(Object value) {
        assertThatThrownBy(() -> AccessTokenVersion.read(token(value))).isInstanceOf(IllegalArgumentException.class);
    }
    private static Stream<Object> invalid() { return Stream.of(-1L, 0.5, "1", new BigInteger("9223372036854775808")); }
    private Jwt token(Object value) { return Jwt.withTokenValue("test").header("alg", "HS256").subject("1").claim(AccessTokenVersion.CLAIM, value).build(); }
}
