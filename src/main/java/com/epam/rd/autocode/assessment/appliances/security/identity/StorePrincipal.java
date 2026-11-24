package com.epam.rd.autocode.assessment.appliances.security.identity;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Objects;

// Password-login adapter whose identity remains stable when the email changes.
public final class StorePrincipal implements UserDetails, CredentialsContainer, AccountPrincipal {
    @Serial private static final long serialVersionUID = 1L;
    private final Long accountId;
    private final User details;

    public StorePrincipal(Long accountId, String username, String passwordHash, AccountRole role) {
        this.accountId = accountId;
        this.details =
                new User(
                        username,
                        passwordHash,
                        AuthorityUtils.createAuthorityList(
                                Objects.requireNonNull(role).authority()));
    }

    public StorePrincipal(Long accountId, String username, String passwordHash, String role) {
        this(accountId, username, passwordHash, AccountRole.valueOf(role));
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public String getUsername() {
        return details.getUsername();
    }

    @Override
    public String getPassword() {
        return details.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return details.getAuthorities();
    }

    @Override
    public boolean isAccountNonExpired() {
        return details.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return details.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return details.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return details.isEnabled();
    }

    @Override
    public void eraseCredentials() {
        details.eraseCredentials();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return accountId != null
                && other instanceof AccountPrincipal principal
                && accountId.equals(principal.getAccountId());
    }

    @Override
    public int hashCode() {
        return accountId == null ? System.identityHashCode(this) : accountId.hashCode();
    }
}
