package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import java.time.Instant;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** Distinct from password authentication so JWT requests never reset password-lock counters. */
public final class AccessTokenAuthentication extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;
    private final AccessTokenPrincipal principal;
    private final Instant expiresAt;

    public AccessTokenAuthentication(AccessTokenPrincipal principal, Collection<? extends GrantedAuthority> authorities, Instant expiresAt) {
        super(authorities); this.principal = principal; this.expiresAt = expiresAt; setAuthenticated(true);
    }
    @Override public AccessTokenPrincipal getPrincipal() { return principal; }
    public Instant getExpiresAt() { return expiresAt; }
    @Override public Object getCredentials() { return ""; }
}
