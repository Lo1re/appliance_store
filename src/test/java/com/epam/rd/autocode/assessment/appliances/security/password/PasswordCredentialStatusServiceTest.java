package com.epam.rd.autocode.assessment.appliances.security.password;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.model.security.PasswordCredentialState;
import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordCredentialStateRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class PasswordCredentialStatusServiceTest {
    private final PasswordCredentialStateRepository states =
            mock(PasswordCredentialStateRepository.class);
    private final OAuthIdentityRepository identities = mock(OAuthIdentityRepository.class);
    private final PasswordCredentialStatusService service =
            new PasswordCredentialStatusService(states, identities);

    @Test
    void preservesLegacyPasswordAccessWithoutGuessingFromTheHash() {
        when(states.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.status(1L)).isEqualTo(PasswordCredentialStatus.SET);
        when(identities.existsByProviderAndUserId("GOOGLE", 1L)).thenReturn(true);
        assertThat(service.status(1L)).isEqualTo(PasswordCredentialStatus.UNKNOWN);
        assertThat(service.explicitlyMissing(1L)).isFalse();
    }

    @Test
    void explicitGooglePasswordStateChangesOnlyWhenARealPasswordWasSet() {
        var state = new PasswordCredentialState(1L, false);
        when(states.findById(1L)).thenReturn(Optional.of(state));
        assertThat(service.status(1L)).isEqualTo(PasswordCredentialStatus.NOT_SET);
        assertThat(service.explicitlyMissing(1L)).isTrue();
        service.markSet(1L);
        assertThat(service.status(1L)).isEqualTo(PasswordCredentialStatus.SET);
        assertThat(service.explicitlyMissing(1L)).isFalse();
    }

    @Test
    void linkingAnAccountDoesNotOverwriteAnExistingPasswordlessMarker() {
        when(states.existsById(1L)).thenReturn(true);
        service.rememberExistingPassword(1L);
        verify(states, never()).save(any());
    }
}
