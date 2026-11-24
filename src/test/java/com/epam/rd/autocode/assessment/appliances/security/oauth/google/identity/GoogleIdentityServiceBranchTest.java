package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.oauth.OAuthIdentity;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class GoogleIdentityServiceBranchTest {
    final OAuthIdentityRepository identities = mock(OAuthIdentityRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final ClientRepository clients = mock(ClientRepository.class);
    final AccountRowLock accounts = mock(AccountRowLock.class);
    final GoogleIdentityService service =
            new GoogleIdentityService(
                    identities,
                    users,
                    clients,
                    accounts,
                    mock(PasswordEncoder.class),
                    new ResetTokenCodec(),
                    mock(PasswordCredentialStatusService.class),
                    mock(BusinessAudit.class),
                    mock(ApplicationEventPublisher.class));
    final VerifiedGoogleIdentity identity =
            new VerifiedGoogleIdentity("sub", "client@example.test", "Client", true);

    GoogleProfileRequest form() {
        var form = new GoogleProfileRequest();
        form.setName("Client");
        form.setEmail(identity.email());
        form.setCard("CARD");
        return form;
    }

    @Test
    void refusesUnverifiedOrChangedProviderEmail() {
        assertThatThrownBy(
                        () ->
                                service.createProfile(
                                        new VerifiedGoogleIdentity(
                                                "sub", identity.email(), "Client", false),
                                        form()))
                .isInstanceOf(GoogleLoginException.class);
        var form = form();
        form.setEmail("other@example.test");
        assertThatThrownBy(() -> service.createProfile(identity, form))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void refusesAnAlreadyLinkedSubject() {
        when(identities.findByProviderAndSubject("GOOGLE", "sub"))
                .thenReturn(Optional.of(new OAuthIdentity("sub", 1L)));
        assertThatThrownBy(() -> service.createProfile(identity, form()))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void refusesAnExistingCard() {
        when(clients.existsByCard("CARD")).thenReturn(true);
        assertThatThrownBy(() -> service.createProfile(identity, form()))
                .isInstanceOf(AccountException.class)
                .hasMessage("client.card.duplicate");
    }

    @Test
    void availableCardIsAccepted() {
        when(clients.saveAndFlush(any()))
                .thenReturn(new Client(1L, "Client", identity.email(), "hash", "CARD"));
        assertThat(service.createProfile(identity, form())).isEqualTo(1L);
    }

    @Test
    void deletedLinkTargetIsRejected() {
        assertThatThrownBy(() -> service.link(1L, identity))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void linkingSameIdentityIsIdempotentButOtherOwnerIsRejected() {
        when(accounts.lock(1L)).thenReturn(Optional.of(new Client()));
        when(identities.findByProviderAndSubject("GOOGLE", "sub"))
                .thenReturn(Optional.of(new OAuthIdentity("sub", 1L)));
        service.link(1L, identity);
        verify(identities, never()).saveAndFlush(any());
        when(identities.findByProviderAndSubject("GOOGLE", "sub"))
                .thenReturn(Optional.of(new OAuthIdentity("sub", 2L)));
        assertThatThrownBy(() -> service.link(1L, identity))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void accountAlreadyLinkedToDifferentSubjectCannotBeRelinked() {
        when(accounts.lock(1L)).thenReturn(Optional.of(new Client()));
        when(identities.existsByProviderAndUserId("GOOGLE", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.link(1L, identity))
                .isInstanceOf(GoogleLoginException.class);
    }

    @Test
    void knownEmailRequiresExplicitLink() {
        when(users.existsByEmailIgnoreCase(identity.email())).thenReturn(true);
        assertThatThrownBy(() -> service.createProfile(identity, form()))
                .isInstanceOf(GoogleLoginException.class);
    }
}
