package com.epam.rd.autocode.assessment.appliances.security.jwt.config;

import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenClaimsValidator;
import com.epam.rd.autocode.assessment.appliances.security.jwt.api.ApiTransportProperties;
import com.epam.rd.autocode.assessment.appliances.security.jwt.cookie.AuthCookieProperties;
import com.epam.rd.autocode.assessment.appliances.security.jwt.refresh.RefreshTokenProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@ConditionalOnJwt
@EnableConfigurationProperties({JwtProperties.class, ApiTransportProperties.class, AuthCookieProperties.class, RefreshTokenProperties.class})
public class JwtConfiguration {
    @Bean
    public SecretKey accessTokenKey(JwtProperties properties) {
        byte[] key;
        try { key = Base64.getDecoder().decode(properties.getSecretBase64()); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("Configure a Base64 JWT key of at least 32 random bytes"); }
        if (key.length < 32) throw new IllegalArgumentException("JWT signing key requires at least 32 random bytes");
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()
                || properties.getAudience() == null || properties.getAudience().isBlank()
                || properties.getTtl() == null || properties.getTtl().compareTo(Duration.ofSeconds(1)) < 0
                || properties.getTtl().compareTo(Duration.ofMinutes(15)) > 0
                || properties.getClockSkew() == null || properties.getClockSkew().isNegative()
                || properties.getClockSkew().compareTo(Duration.ofMinutes(1)) > 0) {
            throw new IllegalArgumentException("Invalid access token issuer, audience or time policy");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean public JwtEncoder jwtEncoder(SecretKey accessTokenKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenKey));
    }

    @Bean public JwtDecoder jwtDecoder(SecretKey accessTokenKey, JwtProperties properties, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(accessTokenKey).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps = new JwtTimestampValidator(properties.getClockSkew()); timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtIssuerValidator(properties.getIssuer()), new AccessTokenClaimsValidator(properties, clock)));
        return decoder;
    }
}
