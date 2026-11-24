package com.epam.rd.autocode.assessment.appliances.security.registration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.registration.ClientRegistrationRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailLinkFactory;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetRequestLimiter;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

class RegistrationPreparationTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ResetRequestLimiter limits = mock(ResetRequestLimiter.class);
    private final RegistrationPreparation preparation =
            new RegistrationPreparation(
                    users,
                    mock(ClientRepository.class),
                    encoder,
                    new ResetTokenCodec(),
                    limits,
                    new SecurityMailLinkFactory("http://localhost:8080"),
                    Clock.systemUTC());

    private ClientRegistrationRequest form() {
        var form = new ClientRegistrationRequest();
        form.setName("Client");
        form.setEmail("client@example.test");
        form.setCard("CARD");
        form.setNewPassword("PrivatePassword2026!");
        form.setConfirmPassword("PrivatePassword2026!");
        return form;
    }

    @Test
    void throttledSubmissionsDoNotHashPasswordsOrReadAccounts() {
        when(limits.acquire("registration:client@example.test", "registration:127.0.0.1"))
                .thenReturn(42L);
        var result = preparation.prepare(form(), "127.0.0.1");
        assertThat(result.retryAfter()).isEqualTo(42);
        assertThat(result.challenge()).isNull();
        verifyNoInteractions(encoder, users);
    }

    @Test
    void existingEmailStillHashesButDoesNotCreateAMailChallenge() {
        when(users.existsByEmailIgnoreCase("client@example.test")).thenReturn(true);
        assertThatThrownBy(() -> preparation.prepare(form(), "127.0.0.1"))
                .isInstanceOf(AccountException.class)
                .satisfies(
                        failure ->
                                assertThat(((AccountException) failure).error())
                                        .isEqualTo(AccountError.EMAIL_DUPLICATE));
        verify(encoder).encode("PrivatePassword2026!");
    }

    @Test
    void pendingDetailsContainOnlyEncodedPasswordAndHashedVerificationToken() {
        when(encoder.encode("PrivatePassword2026!")).thenReturn("encoded-password");
        var challenge = preparation.prepare(form(), "127.0.0.1").challenge();
        assertThat(challenge.details().passwordHash()).isEqualTo("encoded-password");
        String token = challenge.link().getRawQuery().substring("token=".length());
        assertThat(challenge.tokenHash())
                .isEqualTo(new ResetTokenCodec().hash(token))
                .isNotEqualTo(token);
        assertThat(challenge.toString())
                .doesNotContain(token, "encoded-password", "PrivatePassword2026!");
    }
}
