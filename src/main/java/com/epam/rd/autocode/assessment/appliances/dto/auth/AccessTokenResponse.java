package com.epam.rd.autocode.assessment.appliances.dto.auth;

import java.time.Instant;

/** Token values must not be logged or rendered into the HTML UI. */
public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt) {
    @Override public String toString() { return "AccessTokenResponse[redacted]"; }
}
