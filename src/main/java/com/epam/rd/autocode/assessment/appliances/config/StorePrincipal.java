package com.epam.rd.autocode.assessment.appliances.config;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class StorePrincipal extends User {

    private final Long accountId;

    public StorePrincipal(Long accountId, String username, String passwordHash, String role) {
        super(username, passwordHash, AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}