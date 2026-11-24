package com.epam.rd.autocode.assessment.appliances.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.StorePrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

class DatabaseUserDetailsServiceTest {

    private final UserRepository users = mock(UserRepository.class);

    private final com.epam.rd.autocode.assessment.appliances.security.password
                    .PasswordCredentialStatusService
            passwords =
                    mock(
                            com.epam.rd.autocode.assessment.appliances.security.password
                                    .PasswordCredentialStatusService.class);
    private final DatabaseUserDetailsService service =
            new DatabaseUserDetailsService(users, passwords);

    @Test
    void passwordlessGoogleAccountsCannotAuthenticateWithThePlaceholderHash() {
        when(users.findByEmailIgnoreCase("google@example.test"))
                .thenReturn(
                        Optional.of(
                                new Client(
                                        1L,
                                        "Google",
                                        "google@example.test",
                                        "placeholder-hash",
                                        "card")));
        when(passwords.explicitlyMissing(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.loadUserByUsername("google@example.test"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadsClientAndNormalizesEmail() {
        var client = new Client(1L, "Mercury", "mercury@gmail.com", "stored-hash", "5326-1111");

        when(users.findByEmailIgnoreCase("mercury@gmail.com")).thenReturn(Optional.of(client));

        var principal = (StorePrincipal) service.loadUserByUsername("  MERCURY@GMAIL.COM  ");

        assertThat(principal.getAccountId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("mercury@gmail.com");
        assertThat(principal.getPassword()).isEqualTo("stored-hash");
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void loadsEmployee() {
        var employee = new Employee(2L, "Phobos", "phobos@gmail.com", "stored-hash", "salle");

        when(users.findByEmailIgnoreCase("phobos@gmail.com")).thenReturn(Optional.of(employee));

        var principal = (StorePrincipal) service.loadUserByUsername("phobos@gmail.com");

        assertThat(principal.getAccountId()).isEqualTo(2L);
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void rejectsMissingUser() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsUserWithoutSupportedRole() {
        var user = new User(3L, "Unknown", "unknown@example.com", "stored-hash");

        when(users.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankLogin(String username) {
        assertThatThrownBy(() -> service.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class);

        verifyNoInteractions(users);
    }
}
