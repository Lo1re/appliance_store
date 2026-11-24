package com.epam.rd.autocode.assessment.appliances.service.password;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordChangeRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.*;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.projection.CredentialSnapshot;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import com.epam.rd.autocode.assessment.appliances.security.password.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class PasswordChangeServiceBranchTest {
    final CurrentActor actor = mock(CurrentActor.class);
    final AccountRowLock accounts = mock(AccountRowLock.class);
    final UserRepository users = mock(UserRepository.class);
    final PasswordCredentialStatusService state = mock(PasswordCredentialStatusService.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);
    final PasswordChangeService service =
            new PasswordChangeService(
                    actor,
                    accounts,
                    users,
                    mock(CurrentPasswordVerifier.class),
                    encoder,
                    state,
                    mock(PasswordReplacementEffects.class));
    final PasswordChangeRequest form = new PasswordChangeRequest();

    void account() {
        when(actor.requireUser())
                .thenReturn(new Client(1L, "Client", "client@example.test", "old-hash", null));
    }

    @Test
    void missingAccountAfterAuthenticationIsDenied() {
        account();
        assertThatThrownBy(() -> service.change(form)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void passwordlessAccountRequiresSetup() {
        account();
        when(accounts.lock(1L)).thenReturn(Optional.of(new Client()));
        when(state.explicitlyMissing(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.change(form))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage(PasswordChangeError.NOT_SET.messageKey());
    }

    @Test
    void disappearingCredentialSnapshotIsDenied() {
        account();
        when(accounts.lock(1L)).thenReturn(Optional.of(new Client()));
        assertThatThrownBy(() -> service.change(form)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void failedCompareAndSetCannotReportPasswordChanged() {
        account();
        when(accounts.lock(1L)).thenReturn(Optional.of(new Client()));
        when(users.findCredentialSnapshot(1L))
                .thenReturn(Optional.of(new CredentialSnapshot("client@example.test", "old-hash")));
        assertThatThrownBy(() -> service.change(form))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage(PasswordChangeError.CURRENT_INVALID.messageKey());
        verify(state, never()).markSet(1L);
    }
}
