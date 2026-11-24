package com.epam.rd.autocode.assessment.appliances.security.jwt.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.function.Consumer;

class JwtPolicyEdgesTest {
    JwtProperties properties() {
        var p = new JwtProperties();
        p.setSecretBase64(Base64.getEncoder().encodeToString(new byte[32]));
        return p;
    }

    void rejects(Consumer<JwtProperties> invalid) {
        var p = properties();
        invalid.accept(p);
        assertThatThrownBy(() -> new JwtConfiguration().accessTokenKey(p))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEveryInvalidIssuerAudienceAndTimeBoundary() {
        rejects(p -> p.setIssuer(null));
        rejects(p -> p.setIssuer(" "));
        rejects(p -> p.setAudience(null));
        rejects(p -> p.setAudience(" "));
        rejects(p -> p.setTtl(null));
        rejects(p -> p.setTtl(Duration.ZERO));
        rejects(p -> p.setTtl(Duration.ofMinutes(16)));
        rejects(p -> p.setClockSkew(null));
        rejects(p -> p.setClockSkew(Duration.ofSeconds(-1)));
        rejects(p -> p.setClockSkew(Duration.ofSeconds(61)));
        assertThat(new JwtConfiguration().accessTokenKey(properties()).getAlgorithm())
                .isEqualTo("HmacSHA256");
    }
}
