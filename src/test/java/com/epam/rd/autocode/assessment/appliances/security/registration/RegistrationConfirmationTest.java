package com.epam.rd.autocode.assessment.appliances.security.registration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.model.registration.PendingRegistration;
import com.epam.rd.autocode.assessment.appliances.repository.registration.PendingRegistrationRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

class RegistrationConfirmationTest {
    private final PendingRegistrationRepository pending = mock(PendingRegistrationRepository.class);
    private final ClientRegistrationService clients = mock(ClientRegistrationService.class);
    private final ResetTokenCodec tokens = new ResetTokenCodec();
    private final Instant now = Instant.parse("2026-11-21T12:00:00Z");
    private final RegistrationConfirmation confirmation =
            new RegistrationConfirmation(
                    pending, tokens, clients, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void malformedAndMissingTokensCannotAccessAnAccount() {
        assertThatThrownBy(() -> confirmation.complete("wrong"))
                .isInstanceOf(InvalidRegistrationException.class);
        verifyNoInteractions(pending, clients);
        assertThatThrownBy(() -> confirmation.preview(tokens.generate()))
                .isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void expirationIncludesTheExactDeadline() {
        String token = tokens.generate();
        when(pending.lockByHash(tokens.hash(token))).thenReturn(Optional.of(record(token, now)));
        assertThatThrownBy(() -> confirmation.complete(token))
                .isInstanceOf(InvalidRegistrationException.class);
        verifyNoInteractions(clients);
    }

    @Test
    void previewDoesNotConsumeAndCompletionUsesOnlyStoredDetails() {
        String token = tokens.generate();
        var record = record(token, now.plusSeconds(1800));
        when(pending.findById(tokens.hash(token))).thenReturn(Optional.of(record));
        when(pending.lockByHash(tokens.hash(token))).thenReturn(Optional.of(record));
        var details = confirmation.preview(token);
        verifyNoInteractions(clients);
        verify(pending, never()).delete(any());
        when(clients.register(details)).thenReturn(42L);
        assertThat(confirmation.complete(token)).isEqualTo(42L);
        verify(pending).delete(record);
    }

    private PendingRegistration record(String token, Instant expiry) {
        return new PendingRegistration(
                tokens.hash(token), "Client", "client@example.test", null, "bcrypt-hash", expiry);
    }
}
