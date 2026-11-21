package com.epam.rd.autocode.assessment.appliances.security.jwt;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Validates application claims in addition to the standard signature/time/issuer checks. */
@RequiredArgsConstructor
public class AccessTokenClaimsValidator implements OAuth2TokenValidator<Jwt> {
    private final JwtProperties properties;
    private final Clock clock;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean valid;
        try {
            String subject = jwt.getSubject();
            valid = subject != null && subject.matches("[1-9][0-9]{0,18}") && Long.parseLong(subject) > 0
                    && jwt.getAudience() != null && jwt.getAudience().contains(properties.getAudience())
                    && jwt.getIssuedAt() != null && jwt.getExpiresAt() != null
                    && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    && Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).compareTo(properties.getTtl()) <= 0
                    && !jwt.getIssuedAt().isAfter(clock.instant().plus(properties.getClockSkew()))
                    && jwt.getId() != null && !jwt.getId().isBlank()
                    && jwt.getClaims().get("roles") instanceof List<?> roles
                    && roles.stream().allMatch(String.class::isInstance);
        } catch (RuntimeException malformed) { valid = false; }
        return valid ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token", null));
    }
}
