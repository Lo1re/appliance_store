package com.epam.rd.autocode.assessment.appliances.security.jwt.access;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;

import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

public final class AccessTokenPrincipal implements AccountPrincipal, Principal, Serializable {
    private static final long serialVersionUID = 1L;
    private final Long accountId;

    public AccessTokenPrincipal(Long accountId) {
        this.accountId = Objects.requireNonNull(accountId);
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public String getName() {
        return accountId.toString();
    }

    @Override
    public int hashCode() {
        return accountId.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AccountPrincipal principal
                && accountId.equals(principal.getAccountId());
    }
}
