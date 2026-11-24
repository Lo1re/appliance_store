package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

// Local-only credential configuration. Deliberately has no generated toString.
@Getter
@Setter
@ConfigurationProperties("store.security.inmemory")
public class InMemoryAuthenticationProperties {
    private List<Account> accounts = new ArrayList<>();

    @Getter
    @Setter
    public static class Account {
        private String email;
        private String passwordHash;
        private AccountRole role;
    }
}
