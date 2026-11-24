package com.epam.rd.autocode.assessment.appliances.security.jwt.config;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenAuthenticationConverter;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

class JwtCodecTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final JwtProperties properties = properties();
    private final JwtConfiguration config = new JwtConfiguration();
    private final javax.crypto.SecretKey key = config.accessTokenKey(properties);
    private final JwtEncoder encoder = config.jwtEncoder(key);
    private final JwtDecoder decoder =
            config.jwtDecoder(key, properties, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void validatesSignedTokenAndMapsOnlyKnownRolesToStableLocalIdentity() {
        var decoded =
                decoder.decode(
                        sign(
                                claims().claim("roles", List.of("CLIENT", "UNKNOWN")).build(),
                                MacAlgorithm.HS256));
        var authentication = new AccessTokenAuthenticationConverter().convert(decoded);
        assertThat(authentication.getPrincipal().getAccountId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_CLIENT");
        var passwordPrincipal =
                new StorePrincipal(7L, "email@example.com", "hash", AccountRole.CLIENT);
        assertThat(authentication.getPrincipal())
                .isEqualTo(passwordPrincipal)
                .hasSameHashCodeAs(passwordPrincipal);
        assertThat(passwordPrincipal).isEqualTo(authentication.getPrincipal());
    }

    @Test
    void rejectsWrongSignatureUnsignedTokenAndUnapprovedAlgorithm() {
        String token = sign(claims().build(), MacAlgorithm.HS256);
        String[] parts = token.split("\\.");
        String signature = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertThatThrownBy(() -> decoder.decode(parts[0] + "." + parts[1] + "." + signature))
                .isInstanceOf(JwtException.class);
        String plain =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                "{\"alg\":\"none\"}"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> decoder.decode(plain + "." + parts[1] + "."))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(sign(claims().build(), MacAlgorithm.HS384)))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsIncorrectAudienceIssuerSubjectTimesAndMissingExpiry() {
        for (var claims :
                List.of(
                        claims().issuer("other").build(),
                        claims().audience(List.of("other")).build(),
                        claims().subject("not-an-id").build(),
                        claims().subject("0").build(),
                        claims().subject("9999999999999999999").build(),
                        claims().issuedAt(now.minusSeconds(900))
                                .notBefore(now.minusSeconds(900))
                                .expiresAt(now.minusSeconds(60))
                                .build(),
                        claims().notBefore(now.plusSeconds(120)).build(),
                        claims().issuedAt(now.plusSeconds(120)).build(),
                        claims().expiresAt(now.plusSeconds(901)).build(),
                        claims().claims(values -> values.remove("exp")).build())) {
            assertThatThrownBy(() -> decoder.decode(sign(claims, MacAlgorithm.HS256)))
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    void rejectsMissingOrWeakKeysWithoutIncludingTheirValuesInErrors() {
        var invalid = new JwtProperties();
        invalid.setSecretBase64("private-invalid-key");
        assertThatThrownBy(() -> config.accessTokenKey(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("private-invalid-key");
        invalid.setSecretBase64(Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> config.accessTokenKey(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .audience(List.of(properties.getAudience()))
                .subject("7")
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(now.plusSeconds(900))
                .id("test-token-id")
                .claim("roles", List.of("CLIENT"));
    }

    private String sign(JwtClaimsSet claims, MacAlgorithm algorithm) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims))
                .getTokenValue();
    }

    private JwtProperties properties() {
        var value = new JwtProperties();
        value.setSecretBase64(Base64.getEncoder().encodeToString(new byte[64]));
        return value;
    }
}
