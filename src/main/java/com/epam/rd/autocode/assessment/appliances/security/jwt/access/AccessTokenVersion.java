package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import org.springframework.security.oauth2.jwt.Jwt;

// Strict claim parsing. Tokens issued before versioning are version zero until first revocation.
public final class AccessTokenVersion {
    public static final String CLAIM = "credential_version";

    private AccessTokenVersion() {}

    public static long read(Jwt jwt) {
        Object value = jwt.getClaims().get(CLAIM);
        if (value == null) {
            return 0;
        }
        if (!(value instanceof Number) || !value.toString().matches("[0-9]{1,19}")) {
            throw new IllegalArgumentException("Invalid access token version");
        }
        return Long.parseLong(value.toString());
    }
}
