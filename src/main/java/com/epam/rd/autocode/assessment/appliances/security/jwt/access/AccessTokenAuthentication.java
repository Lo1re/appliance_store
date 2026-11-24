package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import lombok.Getter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;

// Distinct from password authentication so JWT requests never reset password-lock counters.
@Getter
public final class AccessTokenAuthentication extends AbstractAuthenticationToken {
    @Serial private static final long serialVersionUID = 1L;
    private final AccessTokenPrincipal principal;
    private final Instant expiresAt;
    private final long credentialVersion;

    public AccessTokenAuthentication(
            AccessTokenPrincipal principal,
            Collection<? extends GrantedAuthority> authorities,
            Instant expiresAt) {
        this(principal, authorities, expiresAt, 0);
    }

    public AccessTokenAuthentication(
            AccessTokenPrincipal principal,
            Collection<? extends GrantedAuthority> authorities,
            Instant expiresAt,
            long credentialVersion) {
        super(authorities);
        this.principal = principal;
        this.expiresAt = expiresAt;
        this.credentialVersion = credentialVersion;
        setAuthenticated(true);
    }

    @Override
    public AccessTokenPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
}
