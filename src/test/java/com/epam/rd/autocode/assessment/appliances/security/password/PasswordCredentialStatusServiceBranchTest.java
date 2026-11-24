package com.epam.rd.autocode.assessment.appliances.security.password;

import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordCredentialStateRepository;

import org.junit.jupiter.api.Test;

class PasswordCredentialStatusServiceBranchTest {
    @Test
    void markingLegacyLocalPasswordDoesNotCreateOAuthMetadata() {
        var states = mock(PasswordCredentialStateRepository.class);
        var identities = mock(OAuthIdentityRepository.class);
        new PasswordCredentialStatusService(states, identities).markSet(1L);
        verify(states, never()).save(any());
    }

    @Test
    void markingLegacyGooglePasswordCreatesExplicitSetMarker() {
        var states = mock(PasswordCredentialStateRepository.class);
        var identities = mock(OAuthIdentityRepository.class);
        when(identities.existsByProviderAndUserId("GOOGLE", 1L)).thenReturn(true);
        new PasswordCredentialStatusService(states, identities).markSet(1L);
        verify(states).save(org.mockito.ArgumentMatchers.argThat(state -> state.isPasswordSet()));
    }
}
