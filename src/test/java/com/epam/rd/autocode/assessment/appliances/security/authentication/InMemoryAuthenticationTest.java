package com.epam.rd.autocode.assessment.appliances.security.authentication;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InMemoryAuthenticationTest {
    private static final String HASH = "$2a$10$PsRnmS7huldXCKf0kWLObOB7ndzQZaCC3Bt/gO6X4kohWInxkC//W";
    private final UserRepository users = mock(UserRepository.class);
    private final InMemoryAuthenticationConfig config = new InMemoryAuthenticationConfig();

    @Test
    void derivesTheRoleFromTheDomainAndUsesConfiguredCredentials() {
        when(users.findByEmailIgnoreCase("client@example.com"))
                .thenReturn(Optional.of(new Client(1L, "Client", "client@example.com", "different-db-hash", "CARD")));
        var properties = properties();
        properties.getAccounts().get(0).setRole(null);
        var details = (StorePrincipal) config.inMemoryUserDetailsService(properties, users, local())
                .loadUserByUsername("  CLIENT@EXAMPLE.COM ");
        assertThat(details.getAccountId()).isEqualTo(1L);
        assertThat(details.getPassword()).isEqualTo(HASH);
        assertThat(details.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_CLIENT");
    }

    @Test
    void missingDomainAccountRefusesLoginButDoesNotPreventStartup() {
        var service = config.inMemoryUserDetailsService(properties(), users, local());
        assertThatThrownBy(() -> service.loadUserByUsername("client@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> service.loadUserByUsername(null)).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void refusesConfiguredRoleMismatchAndChecksItAgainAtLogin() {
        when(users.findByEmailIgnoreCase("client@example.com"))
                .thenReturn(Optional.of(new Employee(1L, "Employee", "client@example.com", "hash", "Sales")));
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(properties(), users, local()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("role");
        when(users.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());
        var service = config.inMemoryUserDetailsService(properties(), users, local());
        when(users.findByEmailIgnoreCase("client@example.com"))
                .thenReturn(Optional.of(new Employee(1L, "Employee", "client@example.com", "hash", "Sales")));
        assertThatThrownBy(() -> service.loadUserByUsername("client@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsNonLocalModePlaintextEmptyAccountsAndCanonicalDuplicatesWithoutExposingCredentials() {
        var prod = local(); prod.setActiveProfiles("local", "prod");
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(properties(), users, prod))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(properties(), users, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(new InMemoryAuthenticationProperties(), users, local()))
                .isInstanceOf(IllegalArgumentException.class);
        var invalid = properties(); invalid.getAccounts().get(0).setPasswordHash("secret-clear-text");
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(invalid, users, local()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("secret-clear-text");
        var duplicate = properties();
        var second = new InMemoryAuthenticationProperties.Account();
        second.setEmail(" CLIENT@EXAMPLE.COM "); second.setPasswordHash(HASH);
        duplicate.setAccounts(List.of(duplicate.getAccounts().get(0), second));
        assertThatThrownBy(() -> config.inMemoryUserDetailsService(duplicate, users, local()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    }

    private MockEnvironment local() { var environment = new MockEnvironment(); environment.setActiveProfiles("local"); return environment; }
    private InMemoryAuthenticationProperties properties() {
        var account = new InMemoryAuthenticationProperties.Account();
        account.setEmail("client@example.com"); account.setPasswordHash(HASH); account.setRole(AccountRole.CLIENT);
        var properties = new InMemoryAuthenticationProperties(); properties.setAccounts(List.of(account)); return properties;
    }
}
