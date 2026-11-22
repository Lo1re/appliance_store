package com.epam.rd.autocode.assessment.appliances.service.impl.account;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountCredentialsServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AccountCredentialsService credentials = new AccountCredentialsService(users, encoder);

    @Test
    void normalizesEmailBeforeCheckingAndAssigningIt() {
        var client = new Client();
        credentials.assignEmail(client, "  BUYER@EXAMPLE.COM  ");
        verify(users).existsByEmailIgnoreCase("buyer@example.com");
        assertThat(client.getEmail()).isEqualTo("buyer@example.com");
    }

    @Test
    void excludesTheCurrentAccountAndLeavesItsEmailUnchangedOnConflict() {
        var client = new Client(7L, "Buyer", "old@example.com", "hash", "CARD");
        when(users.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 7L)).thenReturn(true);
        assertThatThrownBy(() -> credentials.assignEmail(client, " TAKEN@EXAMPLE.COM "))
                .isInstanceOfSatisfying(AccountException.class,
                        error -> assertThat(error.error()).isEqualTo(AccountError.EMAIL_DUPLICATE));
        assertThat(client.getEmail()).isEqualTo("old@example.com");
        credentials.assignEmail(client, " OLD@EXAMPLE.COM ");
        verify(users).existsByEmailIgnoreCaseAndIdNot("old@example.com", 7L);
    }

    @Test
    void storesOnlyTheEncodedPassword() {
        var client = new Client();
        when(encoder.encode("secret")).thenReturn("encoded");
        credentials.setPassword(client, "secret");
        assertThat(client.getPassword()).isEqualTo("encoded");
        verifyNoInteractions(users);
    }
}
