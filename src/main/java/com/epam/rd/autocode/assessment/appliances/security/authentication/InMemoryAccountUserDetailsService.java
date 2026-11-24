package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import com.epam.rd.autocode.assessment.appliances.validation.normalization.EmailAddress;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// Resolves in-memory credentials to the current local account and its domain role.
public class InMemoryAccountUserDetailsService implements UserDetailsService {
    private final InMemoryUserDetailsManager credentials;
    private final UserRepository users;
    private final Map<String, AccountRole> expectedRoles;

    public InMemoryAccountUserDetailsService(
            InMemoryUserDetailsManager credentials,
            UserRepository users,
            Map<String, AccountRole> expectedRoles) {
        this.credentials = credentials;
        this.users = users;
        this.expectedRoles = Map.copyOf(expectedRoles);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw missing();
        }
        String email = EmailAddress.normalize(username);
        UserDetails configured = credentials.loadUserByUsername(email);
        var account = users.findByEmailIgnoreCase(email).orElseThrow(this::missing);
        AccountRole role = AccountRole.from(account).orElseThrow(this::missing);
        AccountRole expected = expectedRoles.get(email);
        if (expected != null && expected != role) {
            throw missing();
        }
        return new StorePrincipal(
                account.getId(), account.getEmail(), configured.getPassword(), role);
    }

    private UsernameNotFoundException missing() {
        return new UsernameNotFoundException("Account not found");
    }
}
