package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RefreshTokenCodecTest {
    private final RefreshTokenCodec codec = new RefreshTokenCodec();
    @Test void generatesDistinctCanonicalOpaqueCredentialsAndStableOneWayFingerprints() {
        String first = codec.generate(), second = codec.generate();
        assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);
        assertThat(java.util.Base64.getUrlDecoder().decode(first)).hasSize(32);
        assertThat(codec.hash(first)).matches("[0-9a-f]{64}").isEqualTo(codec.hash(first)).isNotEqualTo(first).isNotEqualTo(codec.hash(second));
        assertThatThrownBy(() -> codec.hash(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> codec.hash(first + "=")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> codec.hash("secret-that-must-not-appear"))
                .isInstanceOf(InvalidRefreshTokenException.class).hasMessageNotContaining("secret-that-must-not-appear");
    }
}
