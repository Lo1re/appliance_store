package com.epam.rd.autocode.assessment.appliances.dto.auth;

import java.time.Instant;

/** Only the explicitly selected header transport may serialize credential values. */
public record TokenPairResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt,
                                String refreshToken, Instant refreshExpiresAt) {
    @Override public String toString() { return "TokenPairResponse[credentials=REDACTED]"; }
}
