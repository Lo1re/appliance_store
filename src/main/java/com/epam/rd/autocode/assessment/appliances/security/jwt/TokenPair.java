package com.epam.rd.autocode.assessment.appliances.security.jwt;

import com.epam.rd.autocode.assessment.appliances.dto.auth.AccessTokenResponse;
import java.time.Instant;

/** Internal issuance result; controllers choose a transport explicitly. */
public record TokenPair(AccessTokenResponse access, String refreshToken, Instant refreshExpiresAt) {
    @Override public String toString() { return "TokenPair[credentials=REDACTED]"; }
}
