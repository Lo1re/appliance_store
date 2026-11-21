package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAction;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityNotificationMail;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.oauth.OAuthIdentity;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.oauth.OAuthIdentityRepository;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.ResetTokenCodec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Provider-to-domain mapping; anonymous OIDC onboarding is not an authenticated application service. */
@Component @RequiredArgsConstructor @Validated
@Transactional(readOnly = true)
public class GoogleIdentityService {
    private final OAuthIdentityRepository identities;
    private final UserRepository users;
    private final ClientRepository clients;
    private final AccountRowLock accounts;
    private final PasswordEncoder encoder;
    private final ResetTokenCodec secrets;
    private final PasswordCredentialStatusService passwords;
    private final BusinessAudit audit;
    private final ApplicationEventPublisher events;

    public Optional<Long> findAccount(String subject) {
        return identities.findByProviderAndSubject("GOOGLE", subject).map(OAuthIdentity::getUserId);
    }
    public boolean emailTaken(String email) { return users.existsByEmailIgnoreCase(email); }
    public boolean linked(Long id) { return identities.existsByProviderAndUserId("GOOGLE", id); }

    @Transactional
    public Long createProfile(@NotNull VerifiedGoogleIdentity identity, @Valid @NotNull GoogleProfileRequest profile) {
        if (!identity.emailVerified() || !identity.email().equals(profile.getEmail()))
            throw new GoogleLoginException(GoogleLoginException.Reason.INVALID_IDENTITY);
        if (findAccount(identity.subject()).isPresent()) throw new GoogleLoginException(GoogleLoginException.Reason.LINK_CONFLICT);
        if (emailTaken(identity.email())) throw new GoogleLoginException(GoogleLoginException.Reason.LINK_REQUIRED);
        // No known default password for externally created users; verified reset can establish one later.
        var client = clients.saveAndFlush(new Client(null, profile.getName(), identity.email(),
                encoder.encode(secrets.generate() + "!aA1"), profile.getCard()));
        passwords.initializeGoogle(client.getId());
        identities.saveAndFlush(new OAuthIdentity(identity.subject(), client.getId()));
        audit.record(BusinessAction.CLIENT_CREATED, client.getId());
        events.publishEvent(new SecurityNotificationMail(identity.email(), SecurityNotificationMail.Kind.ACCOUNT_CREATED, LocaleContextHolder.getLocale()));
        return client.getId();
    }

    @Transactional
    public void link(Long accountId, VerifiedGoogleIdentity identity) {
        accounts.lock(accountId).orElseThrow(() -> new GoogleLoginException(GoogleLoginException.Reason.FLOW_EXPIRED));
        var existing = identities.findByProviderAndSubject("GOOGLE", identity.subject());
        if (existing.isPresent() && existing.orElseThrow().getUserId().equals(accountId)) return;
        if (existing.isPresent() || linked(accountId)) throw new GoogleLoginException(GoogleLoginException.Reason.LINK_CONFLICT);
        identities.saveAndFlush(new OAuthIdentity(identity.subject(), accountId));
        passwords.rememberExistingPassword(accountId);
        audit.record(BusinessAction.GOOGLE_LINKED, accountId);
    }
}
